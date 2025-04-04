package io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo

import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener

interface MercuryoBuyRequestInterceptorFactory {
    fun create(onTradeOperationFinishedListener: OnTradeOperationFinishedListener): MercuryoBuyRequestInterceptor
}

interface MercuryoBuyRequestInterceptor : WebViewRequestInterceptor
