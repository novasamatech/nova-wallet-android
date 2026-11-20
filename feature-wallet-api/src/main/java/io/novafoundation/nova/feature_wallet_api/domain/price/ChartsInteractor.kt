package io.novafoundation.nova.feature_wallet_api.domain.price

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.data.repository.duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.CancellationException

interface ChartsInteractor {

    fun chartsFlow(priceId: String): Flow<List<AssetPriceChart>>

    fun chartFlow(priceId: String, currency: Currency, period: PricePeriod): Flow<AssetPriceChart>
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
            val currency = currencyRepository.getSelectedCurrency()
            emitAll(chartFlow(priceId, currency, range))
        }
    }

    override fun chartFlow(priceId: String, currency: Currency, period: PricePeriod): Flow<AssetPriceChart> {
        val sourcePeriod = when (period) {
            PricePeriod.WEEK -> PricePeriod.MONTH
            PricePeriod.YEAR -> PricePeriod.MAX
            else -> period
        }

        return flow {
            emit(AssetPriceChart(period, ExtendedLoadingState.Loading))
            runCatching { coinPriceRepository.getLastHistoryForPeriod(priceId, currency, sourcePeriod) }
                .onSuccess {
                    val chart = AssetPriceChart(sourcePeriod, ExtendedLoadingState.Loaded(it))
                    emit(if (sourcePeriod == period) chart else chart.subChartForPeriod(period))
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    emit(AssetPriceChart(period, ExtendedLoadingState.Error(it)))
                }
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
