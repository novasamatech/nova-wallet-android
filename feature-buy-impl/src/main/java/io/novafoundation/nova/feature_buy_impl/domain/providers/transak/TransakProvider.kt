package io.novafoundation.nova.feature_buy_impl.domain.providers.transak

import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.domain.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.domain.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

const val TRANSAK_NETWORK_KEY = "network"

class TransakProvider(
    private val host: String,
    private val apiKey: String,
    private val environment: String
) : WebViewIntegrationProvider {

    override val id = "transak"
    override val name = "Transak"
    override val officialUrl: String = "transak.com"
    override val logoRes: Int = R.drawable.ic_transak_provider_logo
    override val descriptionRes: Int = R.string.transak_provider_description

    override val supportedFlows = setOf(TradeTokenRegistry.TradeFlow.BUY, TradeTokenRegistry.TradeFlow.SELL)

    override fun getPaymentMethods(tradeFlow: TradeTokenRegistry.TradeFlow): List<TradeTokenRegistry.PaymentMethod> {
        return when (tradeFlow) {
            TradeTokenRegistry.TradeFlow.BUY -> listOf(
                TradeTokenRegistry.PaymentMethod.Visa,
                TradeTokenRegistry.PaymentMethod.MasterCard,
                TradeTokenRegistry.PaymentMethod.ApplePay,
                TradeTokenRegistry.PaymentMethod.GooglePay,
                TradeTokenRegistry.PaymentMethod.Sepa,
                TradeTokenRegistry.PaymentMethod.Other(12)
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
        val network = chainAsset.buyProviders.getValue(id)[TRANSAK_NETWORK_KEY] as? String

        return TransakIntegrator(
            payload = TransakIntegrator.Payload(
                host = host,
                apiKey = apiKey,
                environment = environment,
                network = network,
                tokenSymbol = chainAsset.symbol,
                address = address,
                tradeFlow = tradeFlow
            ),
            onCloseListener,
            onSellOrderCreatedListener
        )
    }
}
