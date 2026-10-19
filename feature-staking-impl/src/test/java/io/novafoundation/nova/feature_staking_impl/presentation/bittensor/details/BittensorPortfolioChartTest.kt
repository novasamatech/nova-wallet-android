package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioValuePoint
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioValueSeries
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorPortfolioChartTest {

    private val first = BittensorPortfolioValuePoint(1000, BigDecimal("12.5"), BigDecimal("4875.25"))

    private val middle = BittensorPortfolioValuePoint(2000, BigDecimal("13"), BigDecimal("5200"))

    private val last = BittensorPortfolioValuePoint(3000, BigDecimal("14"), BigDecimal("5600"))

    private val series = BittensorPortfolioValueSeries(listOf(first, middle, last), null)

    @Test
    fun `selection retains reported TAO without division by present or historical FX`() {
        val chart = chart()
        val selection = select(chart, 0)
        assertEquals(first, chart.selectedPoint(selection))
        assertEquals(BigDecimal("12.5"), chart.selectedPoint(selection)?.tao)
    }

    @Test
    fun `replaced chart rejects old selection even for equal values`() {
        val old = chart()
        val replacement = chart()
        assertNull(replacement.selectedPoint(select(old, 0)))
    }

    @Test
    fun `intermediate selection return is available while overall incomplete coverage stays hidden`() {
        val chart = chart()
        assertNull(chart.change(null))
        assertNull(chart.change(select(chart, 2)))
        val expected = (middle.fiat - first.fiat).divide(first.fiat, 18, java.math.RoundingMode.HALF_EVEN)
        assertEquals(expected, chart.change(select(chart, 1)))
    }

    @Test
    fun `period choices exclude year and all time`() {
        assertEquals(listOf("ONE_DAY", "SEVEN_DAYS", "THIRTY_DAYS", "NINETY_DAYS"), BittensorPortfolioPeriod.entries.map { it.name })
    }

    private fun chart(): BittensorPortfolioChart {
        val state = ExtendedLoadingState.Loaded(series)
        return BittensorPortfolioChart(BittensorPortfolioPeriod.THIRTY_DAYS, state)
    }

    private fun select(chart: BittensorPortfolioChart, index: Int): PriceChartSelection {
        val model = PriceChartModel.Chart("30D", "30D", true, chart.prices)
        return PriceChartSelection(model, chart.prices[index])
    }
}
