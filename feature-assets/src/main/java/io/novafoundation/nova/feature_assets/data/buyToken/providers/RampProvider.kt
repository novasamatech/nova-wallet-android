package io.novafoundation.nova.feature_assets.data.buyToken.providers

import android.content.Context
import android.net.Uri
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.data.buyToken.ExternalProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private const val RAMP_APP_NAME = "Nova Wallet"
private const val RAMP_APP_LOGO = "https://raw.githubusercontent.com/novasamatech/branding/master/logos/Nova_Wallet_Horizontal_On_White_200px.png"

class RampProvider(
    private val host: String,
    private val apiToken: String?
) : ExternalProvider {

    override val id = "ramp"

    override val name: String = "Ramp"

    override val icon: Int = R.drawable.ic_ramp

    override fun createIntegrator(chainAsset: Chain.Asset, address: String): ExternalProvider.Integrator {
        return RampIntegrator(host, apiToken, chainAsset, address)
    }

    class RampIntegrator(
        private val host: String,
        private val apiToken: String?,
        private val chainAsset: Chain.Asset,
        private val address: String
    ) : ExternalProvider.Integrator {

        override fun openBuyFlow(using: Context) {
            using.showBrowser(createPurchaseLink())
        }

        private fun createPurchaseLink(): String {
            return Uri.Builder()
                .scheme("https")
                .authority(host)
                .appendQueryParameter("swapAsset", chainAsset.symbol)
                .appendQueryParameter("userAddress", address)
                .appendNullableQueryParameter("hostApiKey", apiToken)
                .appendQueryParameter("hostAppName", RAMP_APP_NAME)
                .appendQueryParameter("hostLogoUrl", RAMP_APP_LOGO)
                .appendQueryParameter("finalUrl", ExternalProvider.REDIRECT_URL_BASE)
                .build()
                .toString()
        }
    }
}
