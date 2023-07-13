package io.novafoundation.nova.feature_assets.data.buyToken

import androidx.annotation.DrawableRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BuyTokenRegistry(providers: List<Provider<*>>) {

    private val providerById = providers.associateBy(Provider<*>::id)

    fun availableSortedProvidersFor(chainAsset: Chain.Asset) = chainAsset.buyProviders.keys
        .mapNotNull(providerById::get)
        .sortedBy { it.icon }

    interface Provider<I : Integrator<*>> {
        val id: String

        val name: String

        @get:DrawableRes
        val icon: Int

        val priority: Int
            get() = Int.MAX_VALUE

        fun createIntegrator(chainAsset: Chain.Asset, address: String): I
    }

    interface Integrator<T> {

        fun openBuyFlow(using: T)
    }
}
