package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.ChooseAmountView
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

fun BaseFragment<*>.setupAmountChooser(
    mixin: AmountChooserMixin,
    amountView: ChooseAmountView,
) {
    setupAmountChooserBase(mixin, amountView.amountInput)

    mixin.assetModel.observe(amountView::setChooseAmountModel)
    mixin.fiatAmount.observe(amountView::setFiatAmount)
}

fun BaseFragment<*>.setupAmountChooserBase(
    mixin: AmountChooserMixinBase,
    inputField: EditText
) {
    inputField.bindToAmountInput(mixin.inputState, lifecycleScope)
}

private fun EditText.bindToAmountInput(flow: MutableSharedFlow<InputState<String>>, scope: CoroutineScope) {
    bindTo(flow, scope, toT = { InputState(it, initiatedByUser = true) }, fromT = { it.value })
}
