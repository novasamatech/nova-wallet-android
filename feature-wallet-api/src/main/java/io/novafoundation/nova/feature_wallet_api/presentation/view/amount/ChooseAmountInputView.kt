package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_wallet_api.databinding.ViewChooseAmountInputBinding
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountInputModel

class ChooseAmountInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val binder = ViewChooseAmountInputBinding.inflate(inflater(), this)

    val amountInput: EditText
        get() = binder.chooseAmountInputField

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        background = context.getInputBackground()
    }

    // To propagate state_focused from chooseAmountInputField
    override fun childDrawableStateChanged(child: View) {
        refreshDrawableState()
    }

    // Allocate all the state chooseAmountInputField can have, e.g. state_focused and state_enabled
    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val fieldState: IntArray? = amountInput.drawableState

        val need = fieldState?.size ?: 0

        val selfState = super.onCreateDrawableState(extraSpace + need)

        return mergeDrawableStates(selfState, fieldState)
    }

    // Propagate state_enabled to children
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        binder.chooseAmountInputImage.alpha = if (enabled) 1f else 0.48f

        binder.chooseAmountInputField.isEnabled = enabled
        binder.chooseAmountInputToken.isEnabled = enabled
        binder.chooseAmountInputFiat.isEnabled = enabled
    }

    fun loadAssetImage(icon: Icon) {
        binder.chooseAmountInputImage.setTokenIcon(icon, imageLoader)
    }

    fun setAssetName(name: String) {
        binder.chooseAmountInputToken.text = name
    }

    fun setFiatAmount(priceAmount: CharSequence?) {
        binder.chooseAmountInputFiat.setTextOrHide(priceAmount)
    }
}

fun ChooseAmountInputView.setChooseAmountInputModel(model: ChooseAmountInputModel) {
    loadAssetImage(model.tokenIcon)
    setAssetName(model.tokenSymbol)
}
