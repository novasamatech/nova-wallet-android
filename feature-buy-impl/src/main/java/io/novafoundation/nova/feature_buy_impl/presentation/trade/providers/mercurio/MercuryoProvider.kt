package io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.mercurio

import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
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

    override val supportedFlows = setOf(TradeTokenRegistry.TradeType.BUY, TradeTokenRegistry.TradeType.SELL)

    override fun getPaymentMethods(tradeFlow: TradeTokenRegistry.TradeType): List<TradeTokenRegistry.PaymentMethod> {
        return when (tradeFlow) {
            TradeTokenRegistry.TradeType.BUY -> listOf(
                TradeTokenRegistry.PaymentMethod.Visa,
                TradeTokenRegistry.PaymentMethod.MasterCard,
                TradeTokenRegistry.PaymentMethod.ApplePay,
                TradeTokenRegistry.PaymentMethod.GooglePay,
                TradeTokenRegistry.PaymentMethod.Sepa,
                TradeTokenRegistry.PaymentMethod.Other(5)
            )

            TradeTokenRegistry.TradeType.SELL -> listOf(
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
        tradeFlow: TradeTokenRegistry.TradeType,
        onCloseListener: OnTradeOperationFinishedListener,
        onSellOrderCreatedListener: OnSellOrderCreatedListener
    ): WebViewIntegrationProvider.Integrator {
        val network = chainAsset.buyProviders.getValue(id)[MERCURYO_NETWORK_KEY] as? String
        val payload = MercuryoIntegrator.Payload(host, widgetId, chainAsset.symbol, network, address, secret, tradeFlow)
        return integratorFactory.create(payload, onSellOrderCreatedListener, onCloseListener)
    }
}
