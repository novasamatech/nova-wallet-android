package io.novafoundation.nova.feature_buy_api.di

import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi

interface BuyFeatureApi {

    val buyTokenRegistry: TradeTokenRegistry

    val tradeMixinFactory: TradeMixin.Factory

    val buyMixinUi: BuyMixinUi
}
