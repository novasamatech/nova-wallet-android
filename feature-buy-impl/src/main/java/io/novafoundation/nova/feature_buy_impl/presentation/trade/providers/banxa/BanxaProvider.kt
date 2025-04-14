package io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.banxa

import android.net.Uri
import android.webkit.WebView
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_impl.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private const val COIN_KEY = "coinType"
private const val BLOCKCHAIN_KEY = "blockchain"

class BanxaProvider(
    private val host: String
) : WebViewIntegrationProvider {

    override val id: String = "banxa"

    override val name: String = "Banxa"
    override val officialUrl: String = "banxa.com"
    override val logoRes: Int = R.drawable.ic_banxa_provider_logo

    override fun getDescriptionRes(tradeType: TradeTokenRegistry.TradeType): Int {
        return R.string.banxa_provider_description
    }

    override fun getPaymentMethods(tradeType: TradeTokenRegistry.TradeType): List<TradeTokenRegistry.PaymentMethod> {
        return when (tradeType) {
            TradeTokenRegistry.TradeType.BUY -> listOf(
                TradeTokenRegistry.PaymentMethod.Visa,
                TradeTokenRegistry.PaymentMethod.MasterCard,
                TradeTokenRegistry.PaymentMethod.ApplePay,
                TradeTokenRegistry.PaymentMethod.GooglePay,
                TradeTokenRegistry.PaymentMethod.Sepa,
                TradeTokenRegistry.PaymentMethod.Other(5)
            )

            TradeTokenRegistry.TradeType.SELL -> emptyList()
        }
    }

    override fun createIntegrator(
        chainAsset: Chain.Asset,
        address: String,
        tradeFlow: TradeTokenRegistry.TradeType,
        onCloseListener: OnTradeOperationFinishedListener,
        onSellOrderCreatedListener: OnSellOrderCreatedListener
    ): WebViewIntegrationProvider.Integrator {
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
    ) : WebViewIntegrationProvider.Integrator {

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
