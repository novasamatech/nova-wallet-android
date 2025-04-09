package io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.transak

import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
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

    override val supportedFlows = setOf(TradeTokenRegistry.TradeType.BUY, TradeTokenRegistry.TradeType.SELL)

    override fun getPaymentMethods(tradeType: TradeTokenRegistry.TradeType): List<TradeTokenRegistry.PaymentMethod> {
        return when (tradeType) {
            TradeTokenRegistry.TradeType.BUY -> listOf(
                TradeTokenRegistry.PaymentMethod.Visa,
                TradeTokenRegistry.PaymentMethod.MasterCard,
                TradeTokenRegistry.PaymentMethod.ApplePay,
                TradeTokenRegistry.PaymentMethod.GooglePay,
                TradeTokenRegistry.PaymentMethod.Sepa,
                TradeTokenRegistry.PaymentMethod.Other(12)
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
