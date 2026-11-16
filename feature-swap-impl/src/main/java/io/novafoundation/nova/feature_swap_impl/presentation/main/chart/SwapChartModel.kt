package io.novafoundation.nova.feature_swap_impl.presentation.main.chart

import androidx.annotation.ColorRes
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal val swapChartPeriods = listOf(PricePeriod.DAY, PricePeriod.WEEK, PricePeriod.MONTH, PricePeriod.YEAR, PricePeriod.MAX)

data class SwapChartContext(val asset: Chain.Asset?, val currency: Currency, val period: PricePeriod)

class SwapChartPlotModel(
    val context: SwapChartContext,
    val charts: List<PriceChartModel>,
    val failed: Boolean
) {

    val selectedPeriodIndex: Int
        get() = swapChartPeriods.indexOf(context.period)

    val selectedChart: PriceChartModel
        get() = charts[selectedPeriodIndex]
}

data class SwapChartHeaderModel(
    val ticker: String,
    val price: CharSequence,
    val change: String,
    @ColorRes val changeColor: Int,
    val date: String?
)
