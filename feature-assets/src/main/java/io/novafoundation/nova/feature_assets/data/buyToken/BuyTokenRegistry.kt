package io.novafoundation.nova.feature_assets.data.buyToken

import androidx.annotation.DrawableRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BuyTokenRegistry(val availableProviders: List<Provider<*>>) {

    fun availableProviders(chainAsset: Chain.Asset) = availableProviders.filter { it.isTokenSupported(chainAsset) }

    interface Provider<I : Integrator<*>> {
        class UnsupportedTokenException : Exception()

        val supportedTokens: Set<Chain.Asset>

        val name: String

        @get:DrawableRes
        val icon: Int

        fun isTokenSupported(chainAsset: Chain.Asset): Boolean = chainAsset in supportedTokens

        fun createIntegrator(chainAsset: Chain.Asset, address: String): I
    }

    interface Integrator<T> {
        fun integrate(using: T)
    }
}
