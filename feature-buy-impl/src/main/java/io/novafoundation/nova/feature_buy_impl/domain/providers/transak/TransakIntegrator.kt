package io.novafoundation.nova.feature_buy_impl.domain.providers.transak

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_api.domain.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.domain.common.OnSellOrderCreatedListener

private const val JS_BRIDGE_NAME = "TransakAndroidBridge"

class TransakIntegrator(
    private val payload: Payload,
    private val closeListener: OnTradeOperationFinishedListener,
    private val sellOrderCreatedListener: OnSellOrderCreatedListener,
    private val environment: String
) : WebViewIntegrationProvider.Integrator {

    class Payload(
        val host: String,
        val apiKey: String,
        val network: String?,
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
        return Uri.Builder()
            .scheme("https")
            .authority(payload.host)
            .appendQueryParameter("productsAvailed", payload.tradeFlow.getType())
            .appendQueryParameter("apiKey", payload.apiKey)
            .appendQueryParameter("environment", environment)
            .appendQueryParameter("cryptoCurrencyCode", payload.tokenSymbol.value)
            .appendNullableQueryParameter(TRANSAK_NETWORK_KEY, payload.network)
            .appendQueryParameter("walletAddress", payload.address)
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

private class TransakWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        view.loadUrl(request.url.toString())
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        val js = """
            (function() {
                window.addEventListener('message', function(event) {
                    const eventId = event?.data?.event_id;
                    if (eventId) {
                        $JS_BRIDGE_NAME.onTransakEvent(eventId, JSON.stringify(event.data));
                    }
                });
            })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
