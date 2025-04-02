package io.novafoundation.nova.feature_buy_impl.domain.providers.banxa

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.providers.InternalProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

private const val COIN_KEY = "coinType"
private const val BLOCKCHAIN_KEY = "blockchain"

class BanxaProvider(
    private val host: String
) : InternalProvider {

    override val id: String = "banxa"

    override val name: String = "Banxa"
    override val officialUrl: String = "banxa.com"
    override val logoRes: Int = R.drawable.ic_banxa_provider_logo
    override val descriptionRes: Int = R.string.banxa_provider_description

    override val supportedFlows = setOf(TradeTokenRegistry.TradeFlow.BUY)

    override fun getPaymentMethods(tradeFlow: TradeTokenRegistry.TradeFlow): List<TradeTokenRegistry.PaymentMethod> {
        return when (tradeFlow) {
            TradeTokenRegistry.TradeFlow.BUY -> listOf(
                TradeTokenRegistry.PaymentMethod.Visa,
                TradeTokenRegistry.PaymentMethod.MasterCard,
                TradeTokenRegistry.PaymentMethod.ApplePay,
                TradeTokenRegistry.PaymentMethod.GooglePay,
                TradeTokenRegistry.PaymentMethod.Sepa,
                TradeTokenRegistry.PaymentMethod.Other(5)
            )

            TradeTokenRegistry.TradeFlow.SELL -> throw IllegalStateException("Sell is not supported for Banxa provider")
        }
    }

    override fun createIntegrator(chainAsset: Chain.Asset, address: String, tradeFlow: TradeTokenRegistry.TradeFlow): InternalProvider.Integrator {
        val providerDetails = chainAsset.buyProviders.getValue(id)
        val blockchain = providerDetails[BLOCKCHAIN_KEY] as? String
        val coinType = providerDetails[COIN_KEY] as? String
        return BanxaIntegrator(host, blockchain, coinType, address)
    }

    private class BanxaIntegrator(
        private val host: String,
        private val blockchain: String?,
        private val coinType: String?,
        private val address: String
    ) : InternalProvider.Integrator {

        override fun run(using: WebView) {
            using.loadUrl(createLink())
        }

        private fun createLink(): String {
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
