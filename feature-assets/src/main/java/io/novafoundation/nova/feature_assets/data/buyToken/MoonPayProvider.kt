package io.novafoundation.nova.feature_assets.data.buyToken

import android.content.Context
import io.novafoundation.nova.common.utils.hmacSHA256
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.common.utils.toBase64
import io.novafoundation.nova.common.utils.toHexColor
import io.novafoundation.nova.common.utils.urlEncoded
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class MoonPayProvider(
    private val host: String,
    private val privateKey: String,
    private val publicKey: String,
) : ExternalProvider {

    override val id: String = "moonpay"

    override val name: String = "Moonpay"
    override val icon: Int = R.drawable.ic_moonpay

    override fun createIntegrator(chainAsset: Chain.Asset, address: String): ExternalProvider.Integrator {
        return MoonPayIntegrator(host, privateKey, publicKey, chainAsset, address)
    }

    class MoonPayIntegrator(
        private val host: String,
        private val privateKey: String,
        private val publicKey: String,
        private val tokenType: Chain.Asset,
        private val address: String,
    ) : ExternalProvider.Integrator {

        override fun integrate(using: Context) {
            using.showBrowser(createPurchaseLink(using))
        }

        private fun createPurchaseLink(context: Context): String {
            val color = context.getColor(R.color.colorAccent).toHexColor()

            val urlParams = buildString {
                append("?").append("apiKey").append("=").append(publicKey)
                append("&").append("currencyCode").append("=").append(tokenType.symbol)
                append("&").append("walletAddress").append("=").append(address)
                append("&").append("colorCode").append("=").append(color.urlEncoded())
                append("&").append("showWalletAddressForm").append("=").append(true)
                append("&").append("redirectURL").append("=").append(ExternalProvider.REDIRECT_URL_BASE.urlEncoded())
            }

            val signature = urlParams.hmacSHA256(privateKey).toBase64()

            return buildString {
                append("https://").append(host)
                append(urlParams)
                append("&").append("signature").append("=").append(signature.urlEncoded())
            }
        }
    }
}
