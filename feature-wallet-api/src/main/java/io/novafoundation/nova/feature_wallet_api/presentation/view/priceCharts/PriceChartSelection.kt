package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts

/** Original (decimal-valued) point selected in a chart, before conversion to rendering coordinates. */
data class PriceChartSelection(val chart: PriceChartModel.Chart, val point: PriceChartModel.Chart.Price) {

    companion object {

        fun from(chart: PriceChartModel?, point: PriceChartModel.Chart.Price?, isSelected: Boolean): PriceChartSelection? {
            return if (isSelected && chart is PriceChartModel.Chart && point != null && point in chart.priceChart) {
                PriceChartSelection(chart, point)
            } else {
                null
            }
        }
    }
}

fun interface PriceChartSelectionListener {

    /** Null restores the live header on release, cancellation, or replacement of the chart. */
    fun onSelectionChanged(selection: PriceChartSelection?)
}
