package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts

import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.DateChartTextInjector
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.PriceChangeTextInjector
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.PriceTextInjector

class PriceChartTextInjectors(
    val price: PriceTextInjector,
    val priceChange: PriceChangeTextInjector,
    val date: DateChartTextInjector
)
