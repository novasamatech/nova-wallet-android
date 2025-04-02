package io.novafoundation.nova.feature_buy_impl.domain.providers.mercurio

import android.net.Uri
import android.webkit.WebView
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.common.utils.urlEncoded
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.providers.InternalProvider
import io.novafoundation.nova.feature_buy_api.domain.providers.ProviderUtils
import io.novafoundation.nova.feature_buy_impl.domain.providers.mercurio.MercuryoIntegrator.Payload
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.CoroutineScope


class MercuryoIntegratorFactory {

    fun create(payload: Payload): MercuryoIntegrator {
        return MercuryoIntegrator(
            payload
        )
    }
}

class MercuryoIntegrator(
    private val payload: Payload
) : InternalProvider.Integrator {

    class Payload(
        val host: String,
        val widgetId: String,
        val tokenSymbol: TokenSymbol,
        val network: String?,
        val address: String,
        val secret: String,
        val tradeFlow: TradeTokenRegistry.TradeFlow
    )

    override fun run(using: WebView) {
        using.loadUrl(createLink())
    }

    private fun createLink(): String {
        val signature = "${payload.address}${payload.secret}".encodeToByteArray()
            .sha512()
            .toHexString()

        return Uri.Builder()
            .scheme("https")
            .authority(payload.host)
            .appendQueryParameter("widget_id", payload.widgetId)
            .appendQueryParameter("type", payload.tradeFlow.getType())
            .appendNullableQueryParameter(MERCURYO_NETWORK_KEY, payload.network)
            .appendQueryParameter("currency", payload.tokenSymbol.value)
            .appendQueryParameter("address", payload.address)
            .appendQueryParameter("return_url", ProviderUtils.REDIRECT_URL_BASE.urlEncoded())
            .appendQueryParameter("signature", signature)
            .build()
            .toString()
    }

    private fun TradeTokenRegistry.TradeFlow.getType(): String {
        return when (this) {
            TradeTokenRegistry.TradeFlow.BUY -> "buy"
            TradeTokenRegistry.TradeFlow.SELL -> "sell"
        }
    }
}
