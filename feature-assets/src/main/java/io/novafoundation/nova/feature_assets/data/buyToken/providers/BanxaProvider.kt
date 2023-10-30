package io.novafoundation.nova.feature_assets.data.buyToken.providers

import android.content.Context
import android.net.Uri
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.data.buyToken.ExternalProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private const val BLOCKCHAIN_KEY = "blockchain"
private const val COIN_KEY = "coinType"

class BanxaProvider(
    private val host: String
) : ExternalProvider {

    override val id: String = "banxa"

    override val name: String = "Banxa"
    override val officialUrl: String = "banxa.com"
    override val icon: Int = R.drawable.ic_banxa

    override fun createIntegrator(chainAsset: Chain.Asset, address: String): ExternalProvider.Integrator {
        val providerDetails = chainAsset.buyProviders.getValue(id)
        val blockchain = providerDetails[BLOCKCHAIN_KEY] as? String
        val coinType = providerDetails[COIN_KEY] as? String
        return BanxaIntegrator(host, blockchain, coinType, address)
    }

    private class BanxaIntegrator(
        private val host: String,
        private val blockchain: String?,
        private val coinType: String?,
        private val address: String,
    ) : ExternalProvider.Integrator {

        override fun openBuyFlow(using: Context) {
            using.showBrowser(createPurchaseLink())
        }

        private fun createPurchaseLink(): String {
            return Uri.Builder()
                .scheme("https")
                .authority(host)
                .appendNullableQueryParameter(BLOCKCHAIN_KEY, blockchain)
                .appendNullableQueryParameter(COIN_KEY, coinType)
                .appendQueryParameter("walletAddress", address)
                .build()
                .toString()
        }
    }
}
