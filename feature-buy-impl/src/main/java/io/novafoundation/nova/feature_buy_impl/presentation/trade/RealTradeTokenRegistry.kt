package io.novafoundation.nova.feature_buy_impl.presentation.trade

import io.novafoundation.nova.common.utils.intersectsWith
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealTradeTokenRegistry(private val providers: List<TradeTokenRegistry.Provider<*>>) : TradeTokenRegistry {

    override fun hasProvider(chainAsset: Chain.Asset): Boolean {
        val supportedProviderIds = providers.mapToSet { it.id }
        return supportedProviderIds.intersectsWith(chainAsset.buyProviders.keys) ||
            supportedProviderIds.intersectsWith(chainAsset.sellProviders.keys)
    }

    override fun hasProvider(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType): Boolean {
        return availableProvidersFor(chainAsset, tradeType).isNotEmpty()
    }

    override fun availableProvidersFor(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType) = providers
        .filter { provider ->
            val providersByType = when (tradeType) {
                TradeTokenRegistry.TradeType.BUY -> chainAsset.buyProviders
                TradeTokenRegistry.TradeType.SELL -> chainAsset.sellProviders
            }

            provider.id in providersByType
        }
}
