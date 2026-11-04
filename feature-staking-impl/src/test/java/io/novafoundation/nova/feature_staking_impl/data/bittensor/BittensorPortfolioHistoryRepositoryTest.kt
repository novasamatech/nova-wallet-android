package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.math.BigDecimal
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BittensorPortfolioHistoryRepositoryTest {

    private val usd = Currency("USD", "US Dollar", "$", Currency.Category.FIAT, true, 1, "usd", true)

    private val request = BittensorPortfolioHistoryRequest(1L, "chain", "native", usd, BittensorPortfolioPeriod.THIRTY_DAYS)

    private val subjects = mutableListOf<String>()

    private val fxRequests = mutableListOf<Currency>()

    private val fxPeriods = mutableListOf<PricePeriod>()

    private var failure: Exception? = null

    private var stale = false

    private var responseDelay = 0L

    private val api = object : BittensorPortfolioHistoryApi {
        override suspend fun history(url: String): retrofit2.Response<BittensorPortfolioHistoryRemote> {
            val accountSubject = requireNotNull(url.toHttpUrl().queryParameter("accountSubject"))
            val period = requireNotNull(url.toHttpUrl().queryParameter("period"))
            assertEquals("https://infra.example/v1/bittensor/portfolio/history/search", url.substringBefore("?"))
            subjects += accountSubject
            kotlinx.coroutines.delay(responseDelay)
            failure?.let { throw it }
            return retrofit2.Response.success(portfolioHistoryResponse(period = period, freshness = if (stale) "STALE" else "FRESH"))
        }
    }

    private val prices = object : CoinPriceRepository {
        override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? = null

        override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
            assertEquals("native", priceId)
            fxRequests += currency
            fxPeriods += range
            return listOf(HistoricalCoinRate(1000, BigDecimal.TEN))
        }
    }

    private val urls = InfrastructureUrls { "https://infra.example".toHttpUrl() }

    private val backendSource = createBittensorTestSources(urls, portfolioHistoryApi = api)

    private val fxSource = io.novafoundation.nova.feature_staking_impl.data.bittensor.createBittensorTestFxSource(prices)

    private val repository = BittensorPortfolioHistoryRepository(fxSource, backendSource.portfolio)

    private suspend fun load(request: BittensorPortfolioHistoryRequest, account: String) = repository.observe(request, account)
        .first { it.nativePrices == null || it.nativePrices.isNotEmpty() }

    @Test
    fun `saved non USD history renders before a slow FX refresh and recovers on the same stream`() = kotlinx.coroutines.test.runTest {
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        var calls = 0
        val rates = listOf(HistoricalCoinRate(1000, BigDecimal.TEN))
        val delayedPrices = object : CoinPriceRepository by prices {
            override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
                calls++
                release.await()
                return rates
            }
        }
        val fx = createBittensorTestFxSource(delayedPrices)
        val history = BittensorPortfolioHistoryRepository(fx, backendSource.portfolio)
        val nonUsd = request.copy(currency = usd.copy(code = "EUR"))
        val updates = mutableListOf<io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistory>()
        val job = backgroundScope.launch { history.observe(nonUsd, "account-one").toList(updates) }
        testScheduler.runCurrent()
        assertTrue(updates.isNotEmpty())
        assertEquals(emptyList<HistoricalCoinRate>(), updates.last().nativePrices)
        release.complete(Unit)
        testScheduler.runCurrent()
        assertEquals(rates, updates.last().nativePrices)
        assertEquals(1, calls)
        job.cancel()
    }

    @Test
    fun `FX survives restart and offline history remains readable in selected currency`() = kotlinx.coroutines.test.runTest {
        val dao = InMemoryResponseCacheDao()
        val cache = createResponseTestCache(dao)
        val fx = createBittensorTestFxSource(prices, cache)
        val nonUsd = request.copy(currency = usd.copy(code = "EUR"))
        val saved = fx.observe(nonUsd).first { !it.isNullOrEmpty() }
        val offlinePrices = object : CoinPriceRepository by prices {
            override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> =
                throw java.io.IOException("Offline")
        }
        val restartedCache = createResponseTestCache(dao)
        val restarted = createBittensorTestFxSource(offlinePrices, restartedCache)
        assertEquals(saved!!.map { it.timestamp to it.rate }, restarted.observe(nonUsd).first()!!.map { it.timestamp to it.rate })
        assertTrue(restarted.observe(nonUsd.copy(currency = usd.copy(code = "GBP"))).first()!!.isEmpty())
    }

    @Test
    fun `USD uses backend history without FX request`() = runBlocking<Unit> {
        val result = load(request.copy(nativePriceId = null), "account-one")
        assertNull(result.nativePrices)
        assertTrue(result.account.points.isEmpty())
        assertTrue(fxRequests.isEmpty())
        assertTrue(!result.account.status.stale)
    }

    @Test
    fun `currencies accounts and periods remain isolated and use iOS FX sources`() = runBlocking<Unit> {
        val euro = usd.copy(code = "EUR", id = 2, coingeckoId = "eur")
        BittensorPortfolioPeriod.entries.forEach { period ->
            val result = load(request.copy(currency = euro, period = period), "account-one")
            assertEquals(period, result.account.period)
        }
        load(request.copy(metaId = 2L), "account-two")
        assertEquals(listOf(PricePeriod.DAY, PricePeriod.MONTH, PricePeriod.MONTH, PricePeriod.MAX), fxPeriods)
        assertEquals(List(4) { euro }, fxRequests)
        assertEquals(listOf("account-one", "account-one", "account-one", "account-one", "account-two"), subjects)
    }

    @Test
    fun `stale empty remains distinct from failure`() = runBlocking<Unit> {
        stale = true
        val result = load(request, "account-one")
        assertTrue(result.account.status.stale)
        assertTrue(result.account.points.isEmpty())
        failure = IllegalStateException("Offline")
        assertTrue(load(request, "account-one").account.status.stale)
    }

    @Test
    fun `request times out after twenty seconds and exposes a retryable error`() = kotlinx.coroutines.test.runTest {
        responseDelay = 20_001L
        val timedSources = createBittensorTestSources(urls, scope = backgroundScope, portfolioHistoryApi = api)
        val history = BittensorPortfolioHistoryRepository(fxSource, timedSources.portfolio)
        val error = runCatching { history.observe(request, "account-one").first() }.exceptionOrNull()
        assertTrue(error is BittensorTimeoutException)
        assertTrue(error?.cause is kotlinx.coroutines.TimeoutCancellationException)
        assertEquals(20_000L, testScheduler.currentTime)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates`() = runBlocking<Unit> {
        failure = CancellationException("Changed request")
        load(request, "account-one")
    }
}
