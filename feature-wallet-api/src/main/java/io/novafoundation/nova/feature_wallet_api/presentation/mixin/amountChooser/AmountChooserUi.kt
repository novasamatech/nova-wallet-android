package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.ChooseAmountView
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel

fun BaseFragment<*>.setupAmountChooser(
    mixin: AmountChooserMixin,
    amountView: ChooseAmountView,
) {
    amountView.amountInput.bindTo(mixin.amountInput, lifecycleScope)

    mixin.assetModel.observe(amountView::setChooseAmountModel)
    mixin.fiatAmount.observe(amountView::setFiatAmount)
}
