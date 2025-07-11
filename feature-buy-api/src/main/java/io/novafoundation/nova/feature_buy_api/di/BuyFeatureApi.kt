package io.novafoundation.nova.feature_buy_api.di

import io.novafoundation.nova.feature_buy_api.di.deeplinks.BuyDeepLinks
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.mercuryo.MercuryoSignatureFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoBuyRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory

interface BuyFeatureApi {

    val buyTokenRegistry: TradeTokenRegistry

    val tradeMixinFactory: TradeMixin.Factory

    val mercuryoBuyRequestInterceptorFactory: MercuryoBuyRequestInterceptorFactory

    val mercuryoSellRequestInterceptorFactory: MercuryoSellRequestInterceptorFactory

    val buyDeepLinks: BuyDeepLinks

    val mercuryoSignatureFactory: MercuryoSignatureFactory
}
