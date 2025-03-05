package io.novafoundation.nova.feature_assets.domain.price

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_wallet_api.data.repository.PriceChartPeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate

class AssetPriceChart(val range: PriceChartPeriod, val chart: ExtendedLoadingState<List<HistoricalCoinRate>>)
