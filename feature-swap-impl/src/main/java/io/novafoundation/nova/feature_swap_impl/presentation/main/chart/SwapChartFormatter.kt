package io.novafoundation.nova.feature_swap_impl.presentation.main.chart

import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.isNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.price.AssetPriceChart
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModelMapper
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.ChartAxisFormatter
import io.novafoundation.nova.runtime.ext.fullId
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class SwapChartFormatter @Inject constructor(
    private val resourceManager: ResourceManager,
    private val chartMapper: PriceChartModelMapper,
    private val fiatFormatter: FiatFormatter
) {

    fun mapPlot(context: SwapChartContext, history: AssetPriceChart): SwapChartPlotModel {
        val charts = swapChartPeriods.map { period ->
            val chart = if (period == context.period) history else AssetPriceChart(period, ExtendedLoadingState.Loading)
            if (context.asset == null) {
                val loading = chartMapper.map(chart)
                PriceChartModel.Empty(loading.buttonText, resourceManager.getString(R.string.swap_chart_select_token))
            } else {
                chartMapper.map(chart)
            }
        }
        return SwapChartPlotModel(context, charts, failed = history.chart is ExtendedLoadingState.Error)
    }

    fun mapHeader(plot: SwapChartPlotModel, tokenState: ExtendedLoadingState<Token?>, selection: PriceChartSelection?): SwapChartHeaderModel {
        val context = plot.context
        val chart = plot.selectedChart as? PriceChartModel.Chart
        val selectedPoint = selection?.takeIf { it.chart === chart }?.point
        val livePrice = tokenState.dataOrNull?.takeIf {
            it.configuration.fullId == context.asset?.fullId && it.currency.id == context.currency.id
        }?.coinRate?.rate
        val price = selectedPoint?.price ?: livePrice ?: chart?.priceChart?.lastOrNull()?.price
        val change = priceChange(chart?.priceChart?.firstOrNull()?.price, price)

        return SwapChartHeaderModel(
            ticker = context.asset?.let {
                resourceManager.getString(R.string.swap_chart_ticker, it.symbol.value, context.currency.code)
            } ?: resourceManager.getString(R.string.swap_chart_nothing_to_follow),
            price = when {
                context.asset == null -> ""
                price != null -> fiatFormatter.formatFiat(price, context.currency)
                tokenState is ExtendedLoadingState.Loading -> "…"
                else -> "—"
            },
            change = change?.formatAsChange().orEmpty(),
            changeColor = when {
                change == null -> R.color.text_secondary
                change.isNegative -> R.color.text_negative
                change.isPositive -> R.color.text_positive
                else -> R.color.text_secondary
            },
            date = selectedPoint?.let { resourceManager.formatDateTime(it.timestamp * 1000) }
        )
    }

    fun axisFormatter(currency: Currency): ChartAxisFormatter = SwapChartAxisFormatter(fiatFormatter, currency)

    private fun priceChange(firstPrice: BigDecimal?, price: BigDecimal?): BigDecimal? {
        if (firstPrice == null || !firstPrice.isPositive || price == null) return null
        return (price - firstPrice).divide(firstPrice, 18, RoundingMode.HALF_UP) * BigDecimal(100)
    }
}

private class SwapChartAxisFormatter(
    private val fiatFormatter: FiatFormatter,
    private val currency: Currency
) : ChartAxisFormatter {

    override fun format(value: Float): String = fiatFormatter.formatFiat(value.toBigDecimal(), currency).toString()
}
