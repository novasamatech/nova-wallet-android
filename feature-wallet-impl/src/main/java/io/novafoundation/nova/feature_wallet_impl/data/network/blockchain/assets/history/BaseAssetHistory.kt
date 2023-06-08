package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

abstract class BaseAssetHistory(internal val coinPriceRepository: CoinPriceRepository) : AssetHistory {

    protected suspend fun getCoinPriceRange(
        chainAsset: Chain.Asset,
        currency: Currency,
        from: Long?,
        to: Long?
    ): List<HistoricalCoinRate> {
        if (from == null || to == null) return emptyList()
        return runCatching { coinPriceRepository.getCoinPriceRange(chainAsset.priceId!!, currency, from, to) }
            .getOrNull()
            ?: emptyList()
    }
}
