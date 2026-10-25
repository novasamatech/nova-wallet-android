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
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        override suspend fun history(url: String, accountSubject: String, period: String): BittensorPortfolioHistoryRemote {
            assertEquals("https://infra.example/v1/bittensor/portfolio/history/search", url)
            subjects += accountSubject
            kotlinx.coroutines.delay(responseDelay)
            failure?.let { throw it }
            return portfolioHistoryResponse(period = period, freshness = if (stale) "STALE" else "FRESH")
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

    private val repository = BittensorPortfolioHistoryRepository(api, urls, prices)

    @Test
    fun `USD uses backend history without FX request`() = runBlocking<Unit> {
        val result = repository.load(request.copy(nativePriceId = null), "account-one")
        assertNull(result.nativePrices)
        assertTrue(result.account.points.isEmpty())
        assertTrue(fxRequests.isEmpty())
    }

    @Test
    fun `currencies accounts and periods remain isolated and use iOS FX sources`() = runBlocking<Unit> {
        val euro = usd.copy(code = "EUR", id = 2, coingeckoId = "eur")
        BittensorPortfolioPeriod.entries.forEach { period ->
            val result = repository.load(request.copy(currency = euro, period = period), "account-one")
            assertEquals(period, result.account.period)
        }
        repository.load(request.copy(metaId = 2L), "account-two")
        assertEquals(listOf(PricePeriod.DAY, PricePeriod.MONTH, PricePeriod.MONTH, PricePeriod.MAX), fxPeriods)
        assertEquals(List(4) { euro }, fxRequests)
        assertEquals(listOf("account-one", "account-one", "account-one", "account-one", "account-two"), subjects)
    }

    @Test
    fun `stale empty remains distinct from failure`() = runBlocking<Unit> {
        stale = true
        val result = repository.load(request, "account-one")
        assertTrue(result.account.status.stale)
        assertTrue(result.account.points.isEmpty())
        failure = IllegalStateException("Offline")
        assertTrue(runCatching { repository.load(request, "account-one") }.isFailure)
    }

    @Test
    fun `request times out after twenty seconds and exposes a retryable error`() = kotlinx.coroutines.test.runTest {
        responseDelay = 20_001L
        val error = runCatching { repository.load(request, "account-one") }.exceptionOrNull()
        assertTrue(error is BittensorTimeoutException)
        assertTrue(error?.cause is kotlinx.coroutines.TimeoutCancellationException)
        assertEquals(20_000L, testScheduler.currentTime)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates`() = runBlocking<Unit> {
        failure = CancellationException("Changed request")
        repository.load(request, "account-one")
    }
}
