package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
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
        setAddStatesFromChildren(true) // so view will be focused when `chooseAmountInput` is focused

        background = context.getInputBackground()
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
