package io.novafoundation.nova.feature_buy_api.domain

import androidx.annotation.DrawableRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias BuyProvider = BuyTokenRegistry.Provider<*>

interface BuyTokenRegistry {

    fun availableProvidersFor(chainAsset: Chain.Asset) : List<Provider<*>>

    interface Provider<I : Integrator<*>> {
        val id: String

        val name: String

        val officialUrl: String

        @get:DrawableRes
        val icon: Int

        fun createIntegrator(chainAsset: Chain.Asset, address: String): I
    }

    interface Integrator<T> {

        fun openBuyFlow(using: T)
    }
}
