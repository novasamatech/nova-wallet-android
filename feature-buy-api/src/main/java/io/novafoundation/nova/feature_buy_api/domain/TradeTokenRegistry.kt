package io.novafoundation.nova.feature_buy_api.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias TradeProvider = TradeTokenRegistry.Provider<*>

interface TradeTokenRegistry {

    fun hasProvider(chainAsset: Chain.Asset): Boolean

    fun hasProvider(chainAsset: Chain.Asset, tradeFlow: TradeFlow): Boolean

    fun availableProvidersFor(chainAsset: Chain.Asset, tradeFlow: TradeFlow): List<Provider<*>>

    interface Provider<I : Integrator<*>> {
        val id: String

        val name: String

        val officialUrl: String

        val supportedFlows: Set<TradeFlow>

        @get:DrawableRes
        val logoRes: Int

        @get:StringRes
        val descriptionRes: Int

        fun getPaymentMethods(tradeFlow: TradeFlow): List<PaymentMethod>

        fun createIntegrator(chainAsset: Chain.Asset, address: String, tradeFlow: TradeFlow): I
    }

    interface Integrator<T> {

        fun openFlow(using: T)
    }

    enum class TradeFlow {
        BUY, SELL
    }

    sealed interface PaymentMethod {
        object Visa : PaymentMethod
        object MasterCard : PaymentMethod
        object ApplePay : PaymentMethod
        object GooglePay : PaymentMethod
        object Sepa : PaymentMethod
        object BankTransfer : PaymentMethod

        class Other(val quantity: Int) : PaymentMethod
    }
}
