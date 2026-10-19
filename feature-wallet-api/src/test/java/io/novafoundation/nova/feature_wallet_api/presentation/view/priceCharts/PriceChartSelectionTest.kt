package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts

import java.math.BigDecimal
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PriceChartSelectionTest {

    private val point = PriceChartModel.Chart.Price(1_792_400_001L, BigDecimal("123.123456789012345678"))

    private val chart = PriceChartModel.Chart("7D", "7D", true, listOf(point))

    @Test
    fun `selection keeps the original timestamp and decimal value`() {
        val selection = PriceChartSelection.from(chart, point, true)

        assertSame(point, selection?.point)
        assertSame(chart, selection?.chart)
    }

    @Test
    fun `release and cancellation restore live header even when last entry is supplied`() {
        assertNull(PriceChartSelection.from(chart, point, false))
    }

    @Test
    fun `old point is rejected after chart replacement`() {
        val replacementPoint = PriceChartModel.Chart.Price(point.timestamp, point.price)
        val replacement = PriceChartModel.Chart("7D", "7D", true, listOf(replacementPoint))

        assertNull(PriceChartSelection.from(replacement, point, true))
    }

    @Test
    fun `loading empty and absent charts cannot select stale rendered entries`() {
        assertNull(PriceChartSelection.from(PriceChartModel.Loading("7D"), point, true))
        assertNull(PriceChartSelection.from(PriceChartModel.Empty("7D", ""), point, true))
        assertNull(PriceChartSelection.from(null, point, true))
        assertNull(PriceChartSelection.from(chart, null, true))
    }
}
