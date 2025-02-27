package io.novafoundation.nova.feature_assets.presentation.balance.detail

import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.DateChartTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.PriceChangeTextInjector
import io.novafoundation.nova.feature_assets.presentation.views.priceCharts.formatters.PriceTextInjector

class PriceChartTextInjectors(
    val price: PriceTextInjector,
    val priceChange: PriceChangeTextInjector,
    val date: DateChartTextInjector
)
