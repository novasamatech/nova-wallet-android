package io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.mercurio

import android.net.Uri
import android.webkit.WebView
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.urlEncoded
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClient
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.mercuryo.MercuryoSignatureFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.mercuryo.generateMerchantTransactionId
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoBuyRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.ProviderUtils
import io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.mercurio.MercuryoIntegrator.Payload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MercuryoIntegratorFactory(
    private val mercuryoBuyInterceptorFactory: MercuryoBuyRequestInterceptorFactory,
    private val mercuryoSellInterceptorFactory: MercuryoSellRequestInterceptorFactory,
    private val interceptingWebViewClientFactory: InterceptingWebViewClientFactory,
    private val signatureGenerator: MercuryoSignatureFactory
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
            webViewClient,
            signatureGenerator
        )
    }
}

class MercuryoIntegrator(
    private val payload: Payload,
    private val webViewClient: InterceptingWebViewClient,
    private val signatureGenerator: MercuryoSignatureFactory
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

    override suspend fun run(using: WebView) = withContext(Dispatchers.Main) {
        using.webViewClient = webViewClient

        val link = withContext(Dispatchers.IO) { createLink() }
        using.loadUrl(link)
    }

    private suspend fun createLink(): String {
        // Merchant transaction id is a custom id we can provide to mercuryo to track a transaction.
        // Seems useless for us now but required for signature
        val merchantTransactionId = generateMerchantTransactionId()
        val signature = signatureGenerator.createSignature(payload.address, payload.secret, merchantTransactionId)

        val urlBuilder = Uri.Builder()
            .scheme("https")
            .authority(payload.host)
            .appendQueryParameter("widget_id", payload.widgetId)
            .appendQueryParameter("merchant_transaction_id", merchantTransactionId)
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
