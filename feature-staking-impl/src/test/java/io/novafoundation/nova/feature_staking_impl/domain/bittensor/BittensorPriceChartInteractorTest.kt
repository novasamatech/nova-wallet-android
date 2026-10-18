package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlin.time.Duration

class BittensorPriceChartInteractorTest {

    private val currency = mock(Currency::class.java)
    private val logger = mock(Logger::class.java)
    private var requestedPeriod: PricePeriod? = null
    private var failure: Exception? = null
    private var points: List<HistoricalCoinRate> = emptyList()
    private val prices = object : CoinPriceRepository {
        override suspend fun getCoinPriceAtTime(priceId: String, currency: Currency, timestamp: Duration): HistoricalCoinRate? = null

        override suspend fun getLastHistoryForPeriod(priceId: String, currency: Currency, range: PricePeriod): List<HistoricalCoinRate> {
            requestedPeriod = range
            failure?.let { throw it }
            return points
        }
    }

    @Test
    fun `week reuses month source and filters old points`() = runBlocking {
        val now = 2_000_000L
        points = listOf(HistoricalCoinRate(now - 8 * 86_400L, BigDecimal.ONE), HistoricalCoinRate(now, BigDecimal.TEN))
        val states = BittensorPriceChartInteractor(prices, logger).observe("bittensor", currency, PricePeriod.WEEK).take(2).toList()
        assertTrue(states.first().chart is ExtendedLoadingState.Loading)
        assertEquals(PricePeriod.MONTH, requestedPeriod)
        assertEquals(listOf(points.last()), (states.last().chart as ExtendedLoadingState.Loaded).data)
    }

    @Test
    fun `empty response remains empty rather than fabricated chart`() = runBlocking {
        val states = BittensorPriceChartInteractor(prices, logger).observe("bittensor", currency, PricePeriod.DAY).take(2).toList()
        assertEquals(emptyList<HistoricalCoinRate>(), (states.last().chart as ExtendedLoadingState.Loaded).data)
    }

    @Test
    fun `network error exits loading with an error`() = runBlocking {
        failure = IllegalStateException("offline")
        val states = BittensorPriceChartInteractor(prices, logger).observe("bittensor", currency, PricePeriod.MONTH).take(2).toList()
        assertTrue(states.last().chart is ExtendedLoadingState.Error)
        verify(logger).log((states.last().chart as ExtendedLoadingState.Error).exception)
    }

    @Test
    fun `cancellation is not swallowed as a network error`() = runBlocking {
        failure = CancellationException("period changed")
        var cancelled = false
        try {
            BittensorPriceChartInteractor(prices, logger).observe("bittensor", currency, PricePeriod.DAY).take(2).toList()
        } catch (e: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        verifyNoInteractions(logger)
    }
}
