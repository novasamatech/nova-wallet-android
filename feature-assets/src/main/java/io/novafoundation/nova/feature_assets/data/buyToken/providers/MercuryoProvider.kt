package io.novafoundation.nova.feature_assets.data.buyToken.providers

import android.content.Context
import io.novafoundation.nova.common.utils.hmacSHA256
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.common.utils.toBase64
import io.novafoundation.nova.common.utils.toHexColor
import io.novafoundation.nova.common.utils.urlEncoded
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.data.buyToken.ExternalProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.toHexString

class MercuryoProvider(
    private val host: String,
    private val widgetId: String,
    private val secret: String
) : ExternalProvider {

    override val id: String = "mercuryo"

    override val name: String = "mercuryo"
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

            val urlParams = buildString {
                append("?").append("widget_id").append("=").append(widgetId)
                append("&").append("type").append("=").append("buy")
                append("&").append("currency").append("=").append(tokenType.symbol)
                append("&").append("address").append("=").append(address)
                append("&").append("return_url").append("=").append(ExternalProvider.REDIRECT_URL_BASE.urlEncoded())
                append("&").append("signature").append("=").append(signature)
            }

            return buildString {
                append("https://").append(host)
                append(urlParams)
            }
        }
    }
}
