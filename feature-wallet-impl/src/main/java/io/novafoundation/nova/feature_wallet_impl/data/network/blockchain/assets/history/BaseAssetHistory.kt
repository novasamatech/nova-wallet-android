package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.getAllCoinPriceHistory
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

abstract class BaseAssetHistory(internal val coinPriceRepository: CoinPriceRepository) : AssetHistory {

    protected suspend fun getPriceHistory(
        chainAsset: Chain.Asset,
        currency: Currency
    ): List<HistoricalCoinRate> {
        return runCatching { coinPriceRepository.getAllCoinPriceHistory(chainAsset.priceId!!, currency) }
            .getOrNull()
            ?: emptyList()
    }
}
