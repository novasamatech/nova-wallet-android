package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser

import android.view.View.OnClickListener
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.ChooseAmountView
import io.novafoundation.nova.feature_wallet_api.presentation.view.amount.setChooseAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

interface AmountInputView {

    val amountInput: EditText

    fun setFiatAmount(fiat: CharSequence?)

    fun setError(errorState: FieldValidationResult)

    fun setEnabled(enabled: Boolean)
}

interface MaxAvailableView {

    fun setMaxAmountDisplay(maxAmountDisplay: String?)

    fun setMaxActionAvailability(availability: MaxActionAvailability)
}

sealed class MaxActionAvailability {

    class Available(val onMaxClicked: OnClickListener) : MaxActionAvailability()

    object NotAvailable : MaxActionAvailability()
}

fun BaseFragment<*, *>.setupAmountChooser(
    mixin: AmountChooserMixin,
    amountView: ChooseAmountView,
) {
    setupAmountChooserBase(mixin, amountView)

    mixin.assetModel.observe(amountView::setChooseAmountModel)
}

fun <T> BaseFragment<*, *>.setupAmountChooserBase(
    mixin: AmountChooserMixinBase,
    view: T,
) where T : AmountInputView, T : MaxAvailableView {
    setupAmountChooserBase(mixin, view, view)
}

fun BaseFragment<*, *>.setupAmountChooserBase(
    mixin: AmountChooserMixinBase,
    amountInputView: AmountInputView,
    maxAvailableView: MaxAvailableView?
) {
    bindInputStateToField(amountInputView, mixin.inputState, lifecycleScope)
    mixin.fiatAmount.observe(amountInputView::setFiatAmount)
    mixin.fieldError.observe(amountInputView::setError)

    mixin.requestFocusLiveData.observeEvent {
        amountInputView.amountInput.requestFocus()
    }

    if (maxAvailableView == null) return

    mixin.maxAction.display.observe(maxAvailableView::setMaxAmountDisplay)
    mixin.maxAction.maxClick.observe { maxClick ->
        val maxActionAvailability = if (maxClick != null) {
            MaxActionAvailability.Available {
                amountInputView.amountInput.requestFocus()

                maxClick()
            }
        } else {
            MaxActionAvailability.NotAvailable
        }
        maxAvailableView.setMaxActionAvailability(maxActionAvailability)
    }
}

private fun BaseFragment<*, *>.bindInputStateToField(
    amountInputView: AmountInputView,
    flow: MutableSharedFlow<InputState<String>>,
    scope: CoroutineScope
) {
    amountInputView.amountInput.bindTo(flow, scope, toT = { InputState(it, initiatedByUser = true, InputState.InputKind.REGULAR) }, fromT = { it.value })

    flow.observe { amountInputView.setEnabled(it.inputKind.isInputAllowed()) }
}
