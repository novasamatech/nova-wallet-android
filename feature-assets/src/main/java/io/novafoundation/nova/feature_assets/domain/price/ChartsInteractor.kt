package io.novafoundation.nova.feature_assets.domain.price

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.PriceChartPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ChartsInteractor {

    fun chartsFlow(priceId: String): Flow<List<AssetPriceChart>>
}

class RealChartsInteractor(
    private val coinPriceRepository: CoinPriceRepository,
    private val currencyRepository: CurrencyRepository
) : ChartsInteractor {

    override fun chartsFlow(priceId: String): Flow<List<AssetPriceChart>> {
        val ranges = PriceChartPeriod.values()
        return ranges.map { range -> getChartFor(priceId, range) }
            .combine()
    }

    private fun getChartFor(priceId: String, range: PriceChartPeriod): Flow<AssetPriceChart> {
        return flow {
            emit(AssetPriceChart(range, ExtendedLoadingState.Loading))
            val currency = currencyRepository.getSelectedCurrency()

            runCatching { coinPriceRepository.getLastCoinPriceRange(priceId, currency, range) }
                .onSuccess { emit(AssetPriceChart(range, ExtendedLoadingState.Loaded(it))) }
                .onFailure { emit(AssetPriceChart(range, ExtendedLoadingState.Error(it))) }
        }
    }
}
