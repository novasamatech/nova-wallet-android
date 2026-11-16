package io.novafoundation.nova.feature_wallet_api.domain.price

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ChartsInteractorTest {

    private val prices = mock(CoinPriceRepository::class.java)

    private val currencies = mock(CurrencyRepository::class.java)

    private val interactor = RealChartsInteractor(prices, currencies)

    private val eur = Currency("EUR", "Euro", "€", Currency.Category.FIAT, true, 1, "eur", true)

    @Test
    fun `week uses token page month source in the explicitly requested currency`() = runBlocking<Unit> {
        val oldest = point(1)
        val weekStart = point(10)
        val newest = point(16)
        `when`(prices.getLastHistoryForPeriod("coin", eur, PricePeriod.MONTH)).thenReturn(listOf(oldest, weekStart, newest))

        val states = interactor.chartFlow("coin", eur, PricePeriod.WEEK).toList()

        assertSame(ExtendedLoadingState.Loading, states.first().chart)
        val loaded = states.last().chart as ExtendedLoadingState.Loaded
        assertEquals(listOf(weekStart, newest), loaded.data)
        assertEquals(PricePeriod.WEEK, states.last().range)
        verify(prices).getLastHistoryForPeriod("coin", eur, PricePeriod.MONTH)
    }

    @Test
    fun `year uses all history and removes points older than the year`() = runBlocking<Unit> {
        val oldest = point(1)
        val newest = point(400)
        `when`(prices.getLastHistoryForPeriod("coin", eur, PricePeriod.MAX)).thenReturn(listOf(oldest, newest))

        val chart = interactor.chartFlow("coin", eur, PricePeriod.YEAR).last()
        assertEquals(listOf(newest), (chart.chart as ExtendedLoadingState.Loaded).data)
        assertEquals(PricePeriod.YEAR, chart.range)
    }

    @Test
    fun `successfully empty history remains loaded and errors retain their cause`() = runBlocking<Unit> {
        `when`(prices.getLastHistoryForPeriod("coin", eur, PricePeriod.DAY)).thenReturn(emptyList())
        val empty = interactor.chartFlow("coin", eur, PricePeriod.DAY).last().chart
        assertEquals(emptyList<HistoricalCoinRate>(), (empty as ExtendedLoadingState.Loaded).data)

        val failure = IllegalStateException("offline")
        `when`(prices.getLastHistoryForPeriod("coin", eur, PricePeriod.DAY)).thenThrow(failure)
        val error = interactor.chartFlow("coin", eur, PricePeriod.DAY).last().chart
        assertSame(failure, (error as ExtendedLoadingState.Error).exception)
    }

    @Test
    fun `cancelled history is not converted to a retryable error`() = runBlocking<Unit> {
        `when`(prices.getLastHistoryForPeriod("coin", eur, PricePeriod.DAY)).thenThrow(CancellationException("left chart"))
        try {
            interactor.chartFlow("coin", eur, PricePeriod.DAY).toList()
            fail("Expected cancellation")
        } catch (e: CancellationException) {
            assertTrue(e.message!!.contains("left chart"))
        }
    }

    private fun point(day: Long) = HistoricalCoinRate(day * 24 * 60 * 60, day.toBigDecimal())
}
