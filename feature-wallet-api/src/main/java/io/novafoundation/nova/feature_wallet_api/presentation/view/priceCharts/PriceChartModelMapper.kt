package io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.price.AssetPriceChart
import javax.inject.Inject

class PriceChartModelMapper @Inject constructor(private val resourceManager: ResourceManager) {

    fun map(assetPriceChart: AssetPriceChart): PriceChartModel {
        val buttonText = mapButtonText(assetPriceChart.range)

        return when (val chart = assetPriceChart.chart) {
            is ExtendedLoadingState.Loading -> PriceChartModel.Loading(buttonText)

            is ExtendedLoadingState.Error -> PriceChartModel.Empty(buttonText, resourceManager.getString(R.string.price_chart_load_failed))

            // Price feed may successfully return no points at all - for example, when the feed is stale or the asset has just been listed
            is ExtendedLoadingState.Loaded -> if (chart.data.isEmpty()) {
                PriceChartModel.Empty(buttonText, resourceManager.getString(R.string.price_chart_no_data))
            } else {
                val periodName = mapPeriodName(assetPriceChart.range)
                val supportTimeShowing = supportTimeShowing(assetPriceChart.range)
                val mappedChart = chart.data.map { PriceChartModel.Chart.Price(it.timestamp, it.rate) }
                PriceChartModel.Chart(buttonText, periodName, supportTimeShowing, mappedChart)
            }
        }
    }

    private fun mapButtonText(pricePeriod: PricePeriod): String {
        val buttonTextRes = when (pricePeriod) {
            PricePeriod.DAY -> R.string.price_chart_day
            PricePeriod.WEEK -> R.string.price_chart_week
            PricePeriod.MONTH -> R.string.price_chart_month
            PricePeriod.YEAR -> R.string.price_chart_year
            PricePeriod.MAX -> R.string.price_chart_max
        }

        return resourceManager.getString(buttonTextRes)
    }

    private fun mapPeriodName(pricePeriod: PricePeriod): String {
        val periodNameRes = when (pricePeriod) {
            PricePeriod.DAY -> R.string.price_charts_period_today
            PricePeriod.WEEK -> R.string.price_charts_period_week
            PricePeriod.MONTH -> R.string.price_charts_period_month
            PricePeriod.YEAR -> R.string.price_charts_period_year
            PricePeriod.MAX -> R.string.price_charts_period_all
        }

        return resourceManager.getString(periodNameRes)
    }

    private fun supportTimeShowing(pricePeriod: PricePeriod): Boolean {
        return when (pricePeriod) {
            PricePeriod.DAY, PricePeriod.WEEK, PricePeriod.MONTH -> true
            PricePeriod.YEAR, PricePeriod.MAX -> false
        }
    }
}
