package io.novafoundation.nova.feature_assets.data.buyToken

import androidx.annotation.DrawableRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BuyTokenRegistry(providers: List<Provider<*>>) {

    private val providerById = providers.associateBy(Provider<*>::id)

    fun availableProvidersFor(chainAsset: Chain.Asset) = chainAsset.buyProviders.keys.mapNotNull(providerById::get)

    interface Provider<I : Integrator<*>> {
        class UnsupportedTokenException : Exception()

        val id: String

        val name: String

        @get:DrawableRes
        val icon: Int

        fun createIntegrator(chainAsset: Chain.Asset, address: String): I
    }

    interface Integrator<T> {

        fun openBuyFlow(using: T)
    }
}
