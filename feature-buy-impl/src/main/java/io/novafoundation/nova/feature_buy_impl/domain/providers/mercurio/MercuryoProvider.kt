package io.novafoundation.nova.feature_buy_impl.domain.providers.mercurio

import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.domain.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.domain.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

const val MERCURYO_NETWORK_KEY = "network"

class MercuryoProvider(
    private val host: String,
    private val widgetId: String,
    private val secret: String,
    private val integratorFactory: MercuryoIntegratorFactory
) : WebViewIntegrationProvider {

    override val id: String = "mercuryo"

    override val name: String = "Mercuryo"
    override val officialUrl: String = "mercuryo.io"
    override val logoRes: Int = R.drawable.ic_mercurio_provider_logo
    override val descriptionRes: Int = R.string.mercurio_provider_description

    override val supportedFlows = setOf(TradeTokenRegistry.TradeFlow.BUY, TradeTokenRegistry.TradeFlow.SELL)

    override fun getPaymentMethods(tradeFlow: TradeTokenRegistry.TradeFlow): List<TradeTokenRegistry.PaymentMethod> {
        return when (tradeFlow) {
            TradeTokenRegistry.TradeFlow.BUY -> listOf(
                TradeTokenRegistry.PaymentMethod.Visa,
                TradeTokenRegistry.PaymentMethod.MasterCard,
                TradeTokenRegistry.PaymentMethod.ApplePay,
                TradeTokenRegistry.PaymentMethod.GooglePay,
                TradeTokenRegistry.PaymentMethod.Sepa,
                TradeTokenRegistry.PaymentMethod.Other(5)
            )

            TradeTokenRegistry.TradeFlow.SELL -> listOf(
                TradeTokenRegistry.PaymentMethod.Visa,
                TradeTokenRegistry.PaymentMethod.MasterCard,
                TradeTokenRegistry.PaymentMethod.Sepa,
                TradeTokenRegistry.PaymentMethod.BankTransfer
            )
        }
    }

    override fun createIntegrator(
        chainAsset: Chain.Asset,
        address: String,
        tradeFlow: TradeTokenRegistry.TradeFlow,
        onCloseListener: OnTradeOperationFinishedListener,
        onSellOrderCreatedListener: OnSellOrderCreatedListener
    ): WebViewIntegrationProvider.Integrator {
        val network = chainAsset.buyProviders.getValue(id)[MERCURYO_NETWORK_KEY] as? String
        val payload = MercuryoIntegrator.Payload(host, widgetId, chainAsset.symbol, network, address, secret, tradeFlow)
        return integratorFactory.create(payload)
    }
}
