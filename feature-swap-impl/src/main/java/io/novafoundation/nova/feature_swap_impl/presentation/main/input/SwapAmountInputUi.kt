package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAmountInputView
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setupAmountChooserBase
import kotlinx.coroutines.flow.Flow

fun BaseFragment<*>.setupSwapAmountInput(
    mixin: SwapAmountInputMixin,
    amountView: SwapAmountInputView,
) {
    setupSwapAmountInput(mixin, amountView, mixin.assetModel, mixin.fiatAmount)
}

fun BaseFragment<*>.setupSwapAmountInput(
    mixin: SwapAmountInputMixin,
    amountView: SwapAmountInputView,
    assetModel: Flow<SwapAmountInputMixin.SwapInputAssetModel> = mixin.assetModel,
    fiatAmount: Flow<CharSequence> = mixin.fiatAmount
) {
    setupAmountChooserBase(mixin, amountView.amountInput)

    assetModel.observe(amountView::setModel)
    fiatAmount.observe(amountView::setFiatAmount)
}
