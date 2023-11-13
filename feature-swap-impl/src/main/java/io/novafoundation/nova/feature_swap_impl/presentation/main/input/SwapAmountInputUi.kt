package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAmountInputView
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxAvailableView
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooserBase

fun BaseFragment<*>.setupSwapAmountInput(
    mixin: SwapAmountInputMixin,
    amountView: SwapAmountInputView,
    maxAvailableView: MaxAvailableView?
) {
    setupAmountChooserBase(mixin, amountView, maxAvailableView)

    mixin.assetModel.observe(amountView::setModel)
}
