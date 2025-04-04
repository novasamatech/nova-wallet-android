package io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo

import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener

interface MercuryoSellRequestInterceptorFactory {
    fun create(
        tradeSellCallback: OnSellOrderCreatedListener,
        onTradeOperationFinishedListener: OnTradeOperationFinishedListener
    ): MercuryoSellRequestInterceptor
}

interface MercuryoSellRequestInterceptor : WebViewRequestInterceptor
