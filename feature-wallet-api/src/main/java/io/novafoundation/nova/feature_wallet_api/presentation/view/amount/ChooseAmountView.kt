package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ViewChooseAmountBinding
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountInputView
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxAvailableView
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel

class ChooseAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), MaxAvailableView, AmountInputView {

    val binder = ViewChooseAmountBinding.inflate(inflater(), this)

    override val amountInput: EditText
        get() = binder.chooseAmountInput.amountInput

    init {
        attrs?.let(::applyAttrs)
    }

    fun loadAssetImage(icon: Icon) {
        binder.chooseAmountInput.loadAssetImage(icon)
    }

    fun setTitle(title: String?) {
        binder.chooseAmountTitle.setTextOrHide(title)
    }

    fun setAssetName(name: String) {
        binder.chooseAmountInput.setAssetName(name)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        binder.chooseAmountInput.isEnabled = enabled
    }

    override fun setFiatAmount(fiat: CharSequence?) {
        binder.chooseAmountInput.setFiatAmount(fiat)
    }

    override fun setError(errorState: FieldValidationResult) {
        // TODO not implemented
    }

    override fun setMaxAmountDisplay(maxAmountDisplay: String?) {
        binder.chooseAmountMaxButton.setMaxAmountDisplay(maxAmountDisplay)
    }

    override fun setMaxActionAvailability(availability: MaxActionAvailability) {
        binder.chooseAmountMaxButton.isVisible = availability is MaxActionAvailability.Available

        binder.chooseAmountMaxButton.setMaxActionAvailability(availability)
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.ChooseAmountView) {
        isEnabled = it.getBoolean(R.styleable.ChooseAmountView_android_enabled, true)

        val title = it.getString(R.styleable.ChooseAmountView_title) ?: context.getString(R.string.common_amount)
        setTitle(title)
    }
}

fun ChooseAmountView.setChooseAmountModel(chooseAmountModel: ChooseAmountModel) {
    binder.chooseAmountInput.setChooseAmountInputModel(chooseAmountModel.input)
}
