package io.novafoundation.nova.feature_buy_api.presentation.mixin

import android.view.View
import io.novafoundation.nova.common.base.BaseFragment
import kotlinx.coroutines.flow.Flow

interface BuyMixinUi {

    fun setupBuyIntegration(fragment: BaseFragment<*>, mixin: TradeMixin)

    fun setupBuyButton(
        fragment: BaseFragment<*>,
        buyButton: View,
        buyEnabledFlow: Flow<Boolean>,
        customBuyClick: (() -> Unit)
    )
}
