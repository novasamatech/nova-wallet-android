package io.novafoundation.nova.feature_buy_impl.domain.providers.transak

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.providers.InternalProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private const val NETWORK_KEY = "network"

class TransakProvider(
    private val host: String,
    private val apiKey: String,
    private val environment: String
) : InternalProvider {

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

    override fun createIntegrator(chainAsset: Chain.Asset, address: String, tradeFlow: TradeTokenRegistry.TradeFlow): InternalProvider.Integrator {
        val network = chainAsset.buyProviders.getValue(id)[NETWORK_KEY] as? String

        return Integrator(
            host = host,
            apiKey = apiKey,
            environment = environment,
            network = network,
            chainAsset = chainAsset,
            address = address,
            tradeFlow = tradeFlow
        )
    }

    private class Integrator(
        private val host: String,
        private val apiKey: String,
        private val environment: String,
        private val network: String?,
        private val chainAsset: Chain.Asset,
        private val address: String,
        private val tradeFlow: TradeTokenRegistry.TradeFlow
    ) : InternalProvider.Integrator {

        override fun run(using: WebView) {
            using.webViewClient = TransakWebViewClient()
            using.loadUrl(createLink())
        }

        private fun createLink(): String {
            return Uri.Builder()
                .scheme("https")
                .authority(host)
                .appendQueryParameter("uctsAvailed", tradeFlow.getType())
                .appendQueryParameter("apiKey", apiKey)
                .appendQueryParameter("environment", environment)
                .appendQueryParameter("cryptoCurrencyCode", chainAsset.symbol.value)
                .appendNullableQueryParameter(NETWORK_KEY, network)
                .appendQueryParameter("walletAddress", address)
                .appendQueryParameter("disableWalletAddressForm", "true")
                .build()
                .toString()
        }

        private fun TradeTokenRegistry.TradeFlow.getType(): String {
            return when (this) {
                TradeTokenRegistry.TradeFlow.BUY -> "BUY"
                TradeTokenRegistry.TradeFlow.SELL -> "SELL"
            }
        }
    }
}

private class TransakWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        view.loadUrl(request.url.toString())
        return true
    }
}
