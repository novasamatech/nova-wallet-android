package io.novafoundation.nova.feature_buy_api.di

import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi

interface BuyFeatureApi {

    val buyTokenRegistry: BuyTokenRegistry

    val buyMixinFactory: BuyMixin.Factory

    val buyMixinUi: BuyMixinUi
}
