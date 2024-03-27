package io.novafoundation.nova.feature_buy_impl.domain.providers

import android.content.Context
import android.net.Uri
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_buy_api.domain.providers.ExternalProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private const val NETWORK_KEY = "network"

class TransakProvider(
    private val host: String,
    private val apiKey: String,
    private val environment: String
) : ExternalProvider {

    override val id = "transak"
    override val name = "Transak"
    override val officialUrl: String = "transak.com"
    override val icon: Int = R.drawable.ic_transak

    override fun createIntegrator(chainAsset: Chain.Asset, address: String): ExternalProvider.Integrator {
        val network = chainAsset.buyProviders.getValue(id)[NETWORK_KEY] as? String

        return Integrator(
            host = host,
            apiKey = apiKey,
            environment = environment,
            network = network,
            chainAsset = chainAsset,
            address = address
        )
    }

    private class Integrator(
        private val host: String,
        private val apiKey: String,
        private val environment: String,
        private val network: String?,
        private val chainAsset: Chain.Asset,
        private val address: String
    ) : ExternalProvider.Integrator {

        override fun openBuyFlow(using: Context) {
            using.showBrowser(buildPurchaseUrl())
        }

        private fun buildPurchaseUrl(): String {
            return Uri.Builder()
                .scheme("https")
                .authority(host)
                .appendQueryParameter("apiKey", apiKey)
                .appendQueryParameter("environment", environment)
                .appendQueryParameter("cryptoCurrencyCode", chainAsset.symbol.value)
                .appendNullableQueryParameter(NETWORK_KEY, network)
                .appendQueryParameter("walletAddress", address)
                .appendQueryParameter("disableWalletAddressForm", "true")
                .build()
                .toString()
        }
    }
}
