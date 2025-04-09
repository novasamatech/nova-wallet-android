package io.novafoundation.nova.feature_buy_impl.presentation.trade

import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealTradeTokenRegistry(private val providers: List<TradeTokenRegistry.Provider<*>>) : TradeTokenRegistry {

    override fun hasProvider(chainAsset: Chain.Asset): Boolean {
        return providers.any { it.id in chainAsset.buyProviders }
    }

    override fun hasProvider(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType): Boolean {
        return availableProvidersFor(chainAsset, tradeType).isNotEmpty()
    }

    override fun availableProvidersFor(chainAsset: Chain.Asset, tradeType: TradeTokenRegistry.TradeType) = providers
        .filter { tradeType in it.supportedFlows }
        .filter { provider -> provider.id in chainAsset.buyProviders }
}
