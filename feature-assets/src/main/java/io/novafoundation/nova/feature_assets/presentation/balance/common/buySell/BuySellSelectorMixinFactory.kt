package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

class BuySellSelectorMixinFactory(
    private val router: AssetsRouter
) {

    fun create(): BuySellSelectorMixin = RealBuySellSelectorMixin(router)
}
