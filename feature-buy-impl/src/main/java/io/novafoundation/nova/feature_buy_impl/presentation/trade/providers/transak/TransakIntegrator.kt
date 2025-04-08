package io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.transak

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener

// You can find a valid implementation in https://github.com/agtransak/TransakAndroidSample/blob/events/app/src/main/java/com/transak/sample/MainActivity.kt
private const val JS_BRIDGE_NAME = "Android"

class TransakIntegrator(
    private val payload: Payload,
    private val closeListener: OnTradeOperationFinishedListener,
    private val sellOrderCreatedListener: OnSellOrderCreatedListener
) : WebViewIntegrationProvider.Integrator {

    class Payload(
        val host: String,
        val apiKey: String,
        val network: String?,
        val environment: String,
        val tokenSymbol: TokenSymbol,
        val address: String,
        val tradeFlow: TradeTokenRegistry.TradeFlow
    )

    override fun run(using: WebView) {
        using.webViewClient = TransakWebViewClient()
        using.addJavascriptInterface(TransakJsEventBridge(closeListener, sellOrderCreatedListener), JS_BRIDGE_NAME)

        using.loadUrl(createLink())
    }

    private fun createLink(): String {
        val urlBuilder = Uri.Builder()
            .scheme("https")
            .authority(payload.host)
            .appendQueryParameter("productsAvailed", payload.tradeFlow.getType())
            .appendQueryParameter("apiKey", payload.apiKey)
            .appendQueryParameter("environment", payload.environment)
            .appendQueryParameter("cryptoCurrencyCode", payload.tokenSymbol.value)
            .appendNullableQueryParameter(TRANSAK_NETWORK_KEY, payload.network)

        if (payload.tradeFlow == TradeTokenRegistry.TradeFlow.BUY) {
            urlBuilder.appendQueryParameter("walletAddress", payload.address)
                .appendQueryParameter("disableWalletAddressForm", "true")
        }

        return urlBuilder.build().toString()
    }

    private fun TradeTokenRegistry.TradeFlow.getType(): String {
        return when (this) {
            TradeTokenRegistry.TradeFlow.BUY -> "BUY"
            TradeTokenRegistry.TradeFlow.SELL -> "SELL"
        }
    }
}

private class TransakWebViewClient : WebViewClient() {
    // We use it to override base transak loading otherwise transak navigates to android native browser
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        view.loadUrl(request.url.toString())
        return true
    }
}
