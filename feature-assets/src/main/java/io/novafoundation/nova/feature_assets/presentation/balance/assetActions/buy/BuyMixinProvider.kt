package io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_assets.data.buyToken.BuyTokenRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

// TODO wallet - buy
class BuyMixinProvider(
    private val buyTokenRegistry: BuyTokenRegistry,
) : BuyMixin.Presentation {

    override val showProviderChooserEvent = MutableLiveData<Event<BuyMixin.ProviderChooserPayload>>()

    override val integrateWithBuyProviderEvent = MutableLiveData<Event<BuyMixin.IntegrationPayload>>()

    override fun isBuyEnabled(chainId: ChainId, chainAssetId: Int): Boolean {
        return false
    }

    override fun providerChosen(
        provider: BuyTokenRegistry.Provider<*>,
        chainAsset: Chain.Asset,
    ) {
//        val payload = IntegrationPayload(
//            provider = provider,
//            token = token,
//            address = accountAddress
//        )
//
//        integrateWithBuyProviderEvent.value = Event(payload)
    }

    override fun buyClicked(chainId: ChainId, chainAssetId: Int) {
//        val availableProviders = buyTokenRegistry.availableProviders(chainAsset)
//
//        when {
//            availableProviders.isEmpty() -> throw IllegalArgumentException("No provider found for ${token.displayName}")
//            availableProviders.size == 1 -> providerChosen(availableProviders.first(), token, accountAddress)
//            else -> showProviderChooserEvent.value = Event(ProviderChooserPayload(availableProviders, token, accountAddress))
//        }
    }
}
