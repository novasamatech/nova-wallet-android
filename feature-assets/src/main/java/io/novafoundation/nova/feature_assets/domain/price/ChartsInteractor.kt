package io.novafoundation.nova.feature_assets.domain.price

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.data.repository.duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface ChartsInteractor {

    fun chartsFlow(priceId: String): Flow<List<AssetPriceChart>>
}

class RealChartsInteractor(
    private val coinPriceRepository: CoinPriceRepository,
    private val currencyRepository: CurrencyRepository
) : ChartsInteractor {

    override fun chartsFlow(priceId: String): Flow<List<AssetPriceChart>> {
        val dayChart = getChartFor(priceId, PricePeriod.DAY)
        val monthChart = getChartFor(priceId, PricePeriod.MONTH)
        val maxChart = getChartFor(priceId, PricePeriod.MAX)

        val weekChart = monthChart.map { it.subChartForPeriod(PricePeriod.WEEK) }
        val yearChart = maxChart.map { it.subChartForPeriod(PricePeriod.YEAR) }

        return listOf(dayChart, weekChart, monthChart, yearChart, maxChart).combine()
    }

    private fun getChartFor(priceId: String, range: PricePeriod): Flow<AssetPriceChart> {
        return flow {
            emit(AssetPriceChart(range, ExtendedLoadingState.Loading))
            val currency = currencyRepository.getSelectedCurrency()

            runCatching { coinPriceRepository.getLastHistoryForPeriod(priceId, currency, range) }
                .onSuccess { emit(AssetPriceChart(range, ExtendedLoadingState.Loaded(it))) }
                .onFailure { emit(AssetPriceChart(range, ExtendedLoadingState.Error(it))) }
        }
    }

    private fun AssetPriceChart.subChartForPeriod(period: PricePeriod): AssetPriceChart {
        val subChartDuration = period.duration()
        val chart = chart.map { historicalPoints ->
            val lastPoint = historicalPoints.lastOrNull() ?: return@map emptyList()
            val fromDate = lastPoint.timestamp - subChartDuration.inWholeSeconds
            historicalPoints.filter { it.timestamp > fromDate }
        }
        return AssetPriceChart(period, chart)
    }
}
