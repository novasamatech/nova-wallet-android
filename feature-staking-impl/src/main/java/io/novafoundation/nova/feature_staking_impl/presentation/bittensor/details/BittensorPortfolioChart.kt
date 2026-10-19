package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioValuePoint
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioValueSeries
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import java.math.BigDecimal
import java.math.RoundingMode

internal class BittensorPortfolioChart(
    val range: BittensorPortfolioPeriod,
    val chart: ExtendedLoadingState<BittensorPortfolioValueSeries>,
    val request: BittensorPortfolioHistoryRequest? = null
) {

    val prices = chart.dataOrNull?.points.orEmpty().map { PriceChartModel.Chart.Price(it.timestamp, it.fiat) }

    fun selectedPoint(selection: PriceChartSelection?): BittensorPortfolioValuePoint? {
        if (selection == null) return null
        val index = prices.indexOfFirst { it === selection.point }
        return chart.dataOrNull?.points?.getOrNull(index)
    }

    fun change(selection: PriceChartSelection?): BigDecimal? {
        val series = chart.dataOrNull ?: return null
        val point = selectedPoint(selection) ?: return series.change
        if (point === series.points.lastOrNull()) return series.change
        val first = series.points.firstOrNull() ?: return null
        if (!first.fiat.isPositive) return null
        return (point.fiat - first.fiat).divide(first.fiat, 18, RoundingMode.HALF_EVEN)
    }
}
