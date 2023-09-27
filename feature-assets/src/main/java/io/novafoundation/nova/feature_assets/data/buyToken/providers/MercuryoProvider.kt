package io.novafoundation.nova.feature_assets.data.buyToken.providers

import android.content.Context
import android.net.Uri
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.common.utils.urlEncoded
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.data.buyToken.ExternalProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.toHexString

private const val TYPE_BUY = "buy"

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
        return MercuryoIntegrator(host, widgetId, chainAsset, address, secret)
    }

    class MercuryoIntegrator(
        private val host: String,
        private val widgetId: String,
        private val tokenType: Chain.Asset,
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
                .appendQueryParameter("currency", tokenType.symbol)
                .appendQueryParameter("address", address)
                .appendQueryParameter("return_url", ExternalProvider.REDIRECT_URL_BASE.urlEncoded())
                .appendQueryParameter("signature", signature)
                .build()
                .toString()
        }
    }
}
