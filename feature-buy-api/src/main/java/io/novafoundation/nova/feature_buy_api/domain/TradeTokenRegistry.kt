package io.novafoundation.nova.feature_buy_api.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias TradeProvider = TradeTokenRegistry.Provider<*>

interface TradeTokenRegistry {

    fun hasProvider(chainAsset: Chain.Asset): Boolean

    fun hasProvider(chainAsset: Chain.Asset, tradeType: TradeType): Boolean

    fun availableProvidersFor(chainAsset: Chain.Asset, tradeType: TradeType): List<Provider<*>>

    interface Provider<I : Integrator<*>> {
        val id: String

        val name: String

        val officialUrl: String

        val supportedFlows: Set<TradeType>

        @get:DrawableRes
        val logoRes: Int

        @get:StringRes
        val descriptionRes: Int

        fun getPaymentMethods(tradeType: TradeType): List<PaymentMethod>

        fun createIntegrator(chainAsset: Chain.Asset, address: String, tradeType: TradeType): I
    }

    interface Integrator<T> {

        fun openFlow(using: T)
    }

    enum class TradeType {
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
