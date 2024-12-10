package io.novafoundation.nova.feature_swap_impl.presentation.common.views

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIconOrMakeGone
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.getReasonOrNull
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.common.view.shape.getInputBackgroundError
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.ViewSwapAmountInputBinding
import io.novafoundation.nova.feature_swap_impl.presentation.main.input.SwapAmountInputMixin.SwapInputAssetModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountInputView
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputContainer
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputError
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputFiat
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputField
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputImage
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputSubtitle
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputSubtitleImage
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputToken

class SwapAmountInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(context),
    AmountInputView {

    private val binder = ViewSwapAmountInputBinding.inflate(inflater(), this)

    override val amountInput: EditText
        get() = binder.swapAmountInputField

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        binder.swapAmountInputContainer.background = context.getInputBackground()
        binder.swapAmountInputContainer.setAddStatesFromChildren(true)
    }

    fun setSelectTokenClickListener(listener: OnClickListener) {
        binder.swapAmountInputContainer.setOnClickListener(listener)
    }

    fun setModel(model: SwapInputAssetModel) {
        setAssetIcon(model.assetIcon)
        setTitle(model.title)
        setSubtitle(model.subtitleIcon, model.subtitle)
        binder.swapAmountInputFiat.isVisible = model.showInput
        amountInput.isVisible = model.showInput
    }

    override fun setFiatAmount(fiat: CharSequence?) {
        binder.swapAmountInputFiat.text = fiat
    }

    override fun setError(errorState: FieldValidationResult) {
        binder.swapAmountInputError.text = errorState.getReasonOrNull()
        setErrorEnabled(errorState is FieldValidationResult.Error)
    }

    private fun setTitle(title: CharSequence) {
        binder.swapAmountInputToken.text = title
    }

    private fun setSubtitle(icon: Icon?, subtitle: CharSequence) {
        binder.swapAmountInputSubtitleImage.setIconOrMakeGone(icon, imageLoader)
        binder.swapAmountInputSubtitle.text = subtitle
    }

    private fun setAssetIcon(icon: SwapInputAssetModel.SwapAssetIcon) {
        return when (icon) {
            is SwapInputAssetModel.SwapAssetIcon.Chosen -> {
                binder.swapAmountInputImage.setImageTint(null)
                binder.swapAmountInputImage.setTokenIcon(icon.assetIcon, imageLoader)
                binder.swapAmountInputImage.setBackgroundResource(R.drawable.bg_token_container)
            }

            SwapInputAssetModel.SwapAssetIcon.NotChosen -> {
                binder.swapAmountInputImage.setImageTint(context.getColor(R.color.icon_accent))
                binder.swapAmountInputImage.setImageResource(R.drawable.ic_add)
                binder.swapAmountInputImage.setBackgroundResource(R.drawable.ic_swap_asset_default_background)
            }
        }
    }

    fun setErrorEnabled(enabled: Boolean) {
        binder.swapAmountInputError.isVisible = enabled
        if (enabled) {
            amountInput.setTextColor(context.getColor(R.color.text_negative))
            binder.swapAmountInputContainer.background = context.getInputBackgroundError()
        } else {
            amountInput.setTextColor(context.getColor(R.color.text_primary))
            binder.swapAmountInputContainer.background = context.getInputBackground()
        }
    }
}
