package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters

interface ChartAxisFormatter {

    fun format(value: Float): String
}
