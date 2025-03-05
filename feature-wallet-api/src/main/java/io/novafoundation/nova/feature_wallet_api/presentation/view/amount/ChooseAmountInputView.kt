package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountInputModel
import kotlinx.android.synthetic.main.view_choose_amount_input.view.chooseAmountInputFiat
import kotlinx.android.synthetic.main.view_choose_amount_input.view.chooseAmountInputField
import kotlinx.android.synthetic.main.view_choose_amount_input.view.chooseAmountInputImage
import kotlinx.android.synthetic.main.view_choose_amount_input.view.chooseAmountInputToken

class ChooseAmountInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    val amountInput: EditText
        get() = chooseAmountInputField

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_choose_amount_input, this)

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

        chooseAmountInputImage.alpha = if (enabled) 1f else 0.48f

        chooseAmountInputField.isEnabled = enabled
        chooseAmountInputToken.isEnabled = enabled
        chooseAmountInputFiat.isEnabled = enabled
    }

    fun loadAssetImage(icon: Icon) {
        chooseAmountInputImage.setTokenIcon(icon, imageLoader)
    }

    fun setAssetName(name: String) {
        chooseAmountInputToken.text = name
    }

    fun setFiatAmount(priceAmount: CharSequence?) {
        chooseAmountInputFiat.setTextOrHide(priceAmount)
    }
}

fun ChooseAmountInputView.setChooseAmountInputModel(model: ChooseAmountInputModel) {
    loadAssetImage(model.tokenIcon)
    setAssetName(model.tokenSymbol)
}
