package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioFiatHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.math.BigDecimal
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioHistoryRepositoryTest {

    private val usd = Currency(
        code = "USD",
        name = "US Dollar",
        symbol = "$",
        category = Currency.Category.FIAT,
        popular = true,
        id = 1,
        coingeckoId = "usd",
        selected = true
    )

    private val euro = usd.copy(code = "EUR", id = 2, coingeckoId = "eur")

    private val request = BittensorPortfolioHistoryRequest(
        metaId = 1L,
        chainId = "chain",
        nativePriceId = "native",
        currency = usd,
        period = BittensorPortfolioPeriod.THIRTY_DAYS
    )

    private val subjects = mutableListOf<String>()

    private val fxRequests = mutableListOf<Currency>()

    private val fxPeriods = mutableListOf<PricePeriod>()

    private var failure: Exception? = null

    private var stale = false

    private var responseDelay = 0L

    private var fxDelay = 0L

    private var fxFailure: Exception? = null

    private var historyFinished = false

    private var pricesFinished = false

    private var nativePrices = listOf(HistoricalCoinRate(timestamp = 1000, rate = BigDecimal.TEN))

    private val api = object : BittensorPortfolioHistoryApi {
        override suspend fun history(url: String, accountSubject: String, period: String): BittensorPortfolioHistoryRemote {
            assertEquals("https://infra.example/v1/bittensor/portfolio/history/search", url)
            subjects += accountSubject
            try {
                delay(responseDelay)
                failure?.let { throw it }
                return portfolioHistoryResponse(period = period, freshness = if (stale) "STALE" else "FRESH")
            } finally {
                historyFinished = true
            }
        }
    }

    private val prices = object : CoinPriceRepository {
        override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? = null

        override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
            assertEquals("native", priceId)
            fxRequests += currency
            fxPeriods += range
            try {
                delay(fxDelay)
                fxFailure?.let { throw it }
                return nativePrices
            } finally {
                pricesFinished = true
            }
        }
    }

    private val urls = InfrastructureUrls { "https://infra.example".toHttpUrl() }

    private val repository = BittensorPortfolioHistoryRepository(api, urls, prices)

    @Test
    fun `USD uses backend history without FX request regardless of case`() = runTest {
        listOf("USD", "usd").forEach { code ->
            val result = repository.load(request.copy(nativePriceId = null, currency = usd.copy(code = code)), "account-one")
            assertSame(BittensorPortfolioFiatHistory.ReportedUsd, result.fiatHistory)
            assertTrue(result.account.points.isEmpty())
        }
        assertTrue(fxRequests.isEmpty())
    }

    @Test
    fun `currencies accounts and periods remain isolated and use period FX sources`() = runTest {
        BittensorPortfolioPeriod.entries.forEach { period ->
            val result = repository.load(request.copy(currency = euro, period = period), "account-one")
            assertEquals(period, result.account.period)
            assertEquals(BittensorPortfolioFiatHistory.Converted(nativePrices), result.fiatHistory)
        }
        repository.load(request.copy(metaId = 2L), "account-two")
        assertEquals(listOf(PricePeriod.DAY, PricePeriod.MONTH, PricePeriod.MONTH, PricePeriod.MAX), fxPeriods)
        assertEquals(List(4) { euro }, fxRequests)
        assertEquals(listOf("account-one", "account-one", "account-one", "account-one", "account-two"), subjects)
    }

    @Test
    fun `stale empty remains distinct from failure`() = runTest {
        stale = true
        val result = repository.load(request, "account-one")
        assertTrue(result.account.status.stale)
        assertTrue(result.account.points.isEmpty())
        failure = IllegalStateException("Offline")
        val error = runCatching { repository.load(request, "account-one") }.exceptionOrNull()
        assertEquals(failure?.javaClass, error?.javaClass)
        assertEquals(failure?.message, error?.message)
    }

    @Test
    fun `request times out after twenty seconds and exposes a retryable error`() = runTest {
        responseDelay = 20_001L
        val error = runCatching { repository.load(request, "account-one") }.exceptionOrNull()
        assertTrue(error is BittensorTimeoutException)
        assertTrue(error?.cause is TimeoutCancellationException)
        assertEquals(20_000L, testScheduler.currentTime)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates`() = runTest {
        failure = CancellationException("Changed request")
        repository.load(request, "account-one")
    }

    @Test
    fun `invalid account subjects are rejected before fetching`() = runTest {
        listOf("", "a".repeat(129), "has space", "line\nfeed", "non-ascii-\u00e9").forEach { subject ->
            val error = runCatching { repository.load(request, subject) }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
        }
        assertTrue(subjects.isEmpty())
        assertTrue(fxRequests.isEmpty())

        repository.load(request, accountSubject = "!" + "a".repeat(126) + "~")
        assertEquals(128, subjects.single().length)
    }

    @Test
    fun `non USD requires a native price identifier and preserves empty FX observations`() = runTest {
        val error = runCatching {
            repository.load(request.copy(currency = euro, nativePriceId = null), "account-one")
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(fxRequests.isEmpty())

        nativePrices = emptyList()
        val result = repository.load(request.copy(currency = euro), "account-one")
        assertEquals(BittensorPortfolioFiatHistory.Converted(nativePrices), result.fiatHistory)
    }

    @Test
    fun `backend and FX are loaded concurrently`() = runTest {
        responseDelay = 1_000L
        fxDelay = 1_000L
        val result = repository.load(request.copy(currency = euro), "account-one")
        assertEquals(BittensorPortfolioFiatHistory.Converted(nativePrices), result.fiatHistory)
        assertEquals(1_000L, testScheduler.currentTime)
    }

    @Test
    fun `outer cancellation finishes both pending requests`() = runTest {
        responseDelay = Long.MAX_VALUE
        fxDelay = Long.MAX_VALUE
        val load = async { repository.load(request.copy(currency = euro), "account-one") }
        runCurrent()
        assertEquals(listOf("account-one"), subjects)
        assertEquals(listOf(euro), fxRequests)
        assertFalse(historyFinished)
        assertFalse(pricesFinished)

        load.cancelAndJoin()

        assertTrue(load.isCancelled)
        assertTrue(historyFinished)
        assertTrue(pricesFinished)
    }

    @Test
    fun `outer timeout remains cancellation and finishes both requests`() = runTest {
        responseDelay = Long.MAX_VALUE
        fxDelay = Long.MAX_VALUE
        val error = runCatching {
            withTimeout(1_000L) { repository.load(request.copy(currency = euro), "account-one") }
        }.exceptionOrNull()

        assertTrue(error is TimeoutCancellationException)
        assertEquals(1_000L, testScheduler.currentTime)
        assertTrue(historyFinished)
        assertTrue(pricesFinished)
    }

    @Test
    fun `FX failure cancels pending backend history`() = runTest {
        responseDelay = Long.MAX_VALUE
        fxDelay = 1_000L
        fxFailure = IllegalStateException("FX unavailable")
        val error = runCatching { repository.load(request.copy(currency = euro), "account-one") }.exceptionOrNull()

        assertEquals(fxFailure?.javaClass, error?.javaClass)
        assertEquals(fxFailure?.message, error?.message)
        assertEquals(listOf("account-one"), subjects)
        assertTrue(historyFinished)
        assertTrue(pricesFinished)
        assertEquals(1_000L, testScheduler.currentTime)
    }

    @Test
    fun `backend failure cancels pending FX history`() = runTest {
        responseDelay = 1_000L
        fxDelay = Long.MAX_VALUE
        failure = IllegalStateException("Backend unavailable")
        val error = runCatching { repository.load(request.copy(currency = euro), "account-one") }.exceptionOrNull()

        assertEquals(failure?.javaClass, error?.javaClass)
        assertEquals(failure?.message, error?.message)
        assertEquals(listOf(euro), fxRequests)
        assertTrue(historyFinished)
        assertTrue(pricesFinished)
        assertEquals(1_000L, testScheduler.currentTime)
    }

    @Test
    fun `request timeout includes pending FX history`() = runTest {
        fxDelay = Long.MAX_VALUE
        val error = runCatching { repository.load(request.copy(currency = euro), "account-one") }.exceptionOrNull()

        assertTrue(error is BittensorTimeoutException)
        assertTrue(error?.cause is TimeoutCancellationException)
        assertTrue(historyFinished)
        assertTrue(pricesFinished)
        assertEquals(20_000L, testScheduler.currentTime)
    }
}
