package io.novafoundation.nova.feature_buy_impl.domain

import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealBuyTokenRegistry(private val providers: List<BuyTokenRegistry.Provider<*>>) : BuyTokenRegistry {

    override fun availableProvidersFor(chainAsset: Chain.Asset) = providers
        .filter { provider -> provider.id in chainAsset.buyProviders }
}
