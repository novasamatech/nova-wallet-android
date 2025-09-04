package io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.mercurio

import android.net.Uri
import android.webkit.WebView
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.common.utils.urlEncoded
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClient
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoBuyRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.ProviderUtils
import io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.mercurio.MercuryoIntegrator.Payload
import io.novasama.substrate_sdk_android.extensions.toHexString

class MercuryoIntegratorFactory(
    private val mercuryoBuyInterceptorFactory: MercuryoBuyRequestInterceptorFactory,
    private val mercuryoSellInterceptorFactory: MercuryoSellRequestInterceptorFactory,
    private val interceptingWebViewClientFactory: InterceptingWebViewClientFactory,
) {

    fun create(
        payload: Payload,
        onSellOrderCreatedListener: OnSellOrderCreatedListener,
        onCloseListener: OnTradeOperationFinishedListener
    ): MercuryoIntegrator {
        val webViewClient = interceptingWebViewClientFactory.create(
            listOf(
                mercuryoBuyInterceptorFactory.create(onCloseListener),
                mercuryoSellInterceptorFactory.create(onSellOrderCreatedListener, onCloseListener)
            )
        )
        return MercuryoIntegrator(
            payload,
            webViewClient
        )
    }
}

class MercuryoIntegrator(
    private val payload: Payload,
    private val webViewClient: InterceptingWebViewClient
) : WebViewIntegrationProvider.Integrator {

    class Payload(
        val host: String,
        val widgetId: String,
        val tokenSymbol: TokenSymbol,
        val network: String?,
        val address: String,
        val secret: String,
        val tradeFlow: TradeTokenRegistry.TradeType
    )

    override fun run(using: WebView) {
        using.loadUrl(createLink())
        using.webViewClient = webViewClient
    }

    private fun createLink(): String {
        val signature = "${payload.address}${payload.secret}".encodeToByteArray()
            .sha512()
            .toHexString()

        val urlBuilder = Uri.Builder()
            .scheme("https")
            .authority(payload.host)
            .appendQueryParameter("widget_id", payload.widgetId)
            .appendQueryParameter("type", payload.tradeFlow.getType())
            .appendNullableQueryParameter(MERCURYO_NETWORK_KEY, payload.network)
            .appendQueryParameter("currency", payload.tokenSymbol.value)
            .appendQueryParameter("return_url", ProviderUtils.REDIRECT_URL_BASE.urlEncoded())
            .appendQueryParameter("signature", signature)
            .appendQueryParameter("fix_currency", true.toString())

        when (payload.tradeFlow) {
            TradeTokenRegistry.TradeType.BUY -> urlBuilder.appendQueryParameter("address", payload.address)
            TradeTokenRegistry.TradeType.SELL -> urlBuilder.appendQueryParameter("refund_address", payload.address)
                .appendQueryParameter("hide_refund_address", true.toString())
        }

        return urlBuilder.build().toString()
    }

    private fun TradeTokenRegistry.TradeType.getType(): String {
        return when (this) {
            TradeTokenRegistry.TradeType.BUY -> "buy"
            TradeTokenRegistry.TradeType.SELL -> "sell"
        }
    }
}
