package io.novafoundation.nova.feature_buy_impl.presentation.mixin

import android.view.View
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import kotlinx.coroutines.flow.Flow

class RealBuyMixinUi : BuyMixinUi {

    override fun setupBuyButton(
        fragment: BaseFragment<*>,
        buyButton: View,
        buyEnabledFlow: Flow<Boolean>,
        customBuyClick: () -> Unit
    ) = with(fragment) {
        buyButton.setOnClickListener { customBuyClick() }

        buyEnabledFlow.observe(buyButton::setEnabled)
    }
}
