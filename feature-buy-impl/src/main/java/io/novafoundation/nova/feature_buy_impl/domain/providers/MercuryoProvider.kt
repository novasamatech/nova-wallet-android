package io.novafoundation.nova.feature_buy_impl.domain.providers

import android.content.Context
import android.net.Uri
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.common.utils.urlEncoded
import io.novafoundation.nova.feature_buy_api.domain.providers.ExternalProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.toHexString

private const val TYPE_BUY = "buy"
private const val NETWORK_KEY = "network"

class MercuryoProvider(
    private val host: String,
    private val widgetId: String,
    private val secret: String
) : ExternalProvider {

    override val id: String = "mercuryo"

    override val name: String = "Mercuryo"
    override val officialUrl: String = "mercuryo.io"
    override val icon: Int = R.drawable.ic_mercuryo

    override fun createIntegrator(chainAsset: Chain.Asset, address: String): ExternalProvider.Integrator {
        val network = chainAsset.buyProviders.getValue(id)[NETWORK_KEY] as? String
        return MercuryoIntegrator(host, widgetId, chainAsset, network, address, secret)
    }

    class MercuryoIntegrator(
        private val host: String,
        private val widgetId: String,
        private val tokenType: Chain.Asset,
        private val network: String?,
        private val address: String,
        private val secret: String,
    ) : ExternalProvider.Integrator {

        override fun openBuyFlow(using: Context) {
            using.showBrowser(createPurchaseLink())
        }

        private fun createPurchaseLink(): String {
            val signature = "$address$secret".encodeToByteArray()
                .sha512()
                .toHexString()

            return Uri.Builder()
                .scheme("https")
                .authority(host)
                .appendQueryParameter("widget_id", widgetId)
                .appendQueryParameter("type", TYPE_BUY)
                .appendNullableQueryParameter(NETWORK_KEY, network)
                .appendQueryParameter("currency", tokenType.symbol)
                .appendQueryParameter("address", address)
                .appendQueryParameter("return_url", ExternalProvider.REDIRECT_URL_BASE.urlEncoded())
                .appendQueryParameter("signature", signature)
                .build()
                .toString()
        }
    }
}
