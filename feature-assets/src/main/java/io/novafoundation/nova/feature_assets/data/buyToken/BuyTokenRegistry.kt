package io.novafoundation.nova.feature_assets.data.buyToken

import androidx.annotation.DrawableRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BuyTokenRegistry(private val providers: List<Provider<*>>) {

    fun availableProvidersFor(chainAsset: Chain.Asset) = providers.filter { it.isTokenSupported(chainAsset) }

    interface Provider<I : Integrator<*>> {
        class UnsupportedTokenException : Exception()

        val id: String

        val name: String

        @get:DrawableRes
        val icon: Int

        fun isTokenSupported(chainAsset: Chain.Asset): Boolean = id in chainAsset.buyProviders

        fun createIntegrator(chainAsset: Chain.Asset, address: String): I
    }

    interface Integrator<T> {

        fun openBuyFlow(using: T)
    }
}
