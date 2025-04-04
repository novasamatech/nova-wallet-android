package io.novafoundation.nova.feature_buy_api.di

import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoBuyRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory

interface BuyFeatureApi {

    val buyTokenRegistry: TradeTokenRegistry

    val tradeMixinFactory: TradeMixin.Factory

    val buyMixinUi: BuyMixinUi

    val mercuryoBuyRequestInterceptorFactory: MercuryoBuyRequestInterceptorFactory

    val mercuryoSellRequestInterceptorFactory: MercuryoSellRequestInterceptorFactory
}
