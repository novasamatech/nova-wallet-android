package io.novafoundation.nova.feature_swap_impl.presentation.views

import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIconOrMakeGone
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.common.view.shape.getInputBackgroundError
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadTokenIcon
import io.novafoundation.nova.feature_swap_impl.R
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputSubtitleImage
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputFiat
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputField
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputImage
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputSubtitle
import kotlinx.android.synthetic.main.view_swap_amount_input.view.swapAmountInputToken

class SwapAmountInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(
        context
    ) {

    val amountInput: EditText
        get() = swapAmountInputField

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        inflate(context, R.layout.view_swap_amount_input, this)
        background = context.getInputBackground()
        setAddStatesFromChildren(true)
    }

    fun setErrorEnabled(enabled: Boolean) {
        if (enabled) {
            amountInput.setTextColor(context.getColor(R.color.text_negative))
            background = context.getInputBackgroundError()
        } else {
            amountInput.setTextColor(context.getColor(R.color.text_primary))
            background = context.getInputBackground()
        }
    }

    fun setTitle(title: String) {
        swapAmountInputToken.text = title
    }

    fun setSubtitle(icon: Icon?, subtitle: String) {
        swapAmountInputSubtitleImage.setIconOrMakeGone(icon, imageLoader)
        swapAmountInputSubtitle.text = subtitle
    }

    fun setAssetImageUrl(imageUrl: String?) {
        if (imageUrl == null) {
            swapAmountInputImage.setImageTint(context.getColor(R.color.icon_accent))
            swapAmountInputImage.setImageResource(R.drawable.ic_add)
            swapAmountInputImage.setBackgroundResource(R.drawable.ic_swap_asset_default_background)
        } else {
            swapAmountInputImage.setImageTint(context.getColor(R.color.icon_primary))
            swapAmountInputImage.loadTokenIcon(imageUrl, imageLoader)
            swapAmountInputImage.setBackgroundResource(R.drawable.bg_token_container)
        }
    }

    fun setModel(model: Model) {
        setAssetImageUrl(model.assetImageUrl)
        setTitle(model.title)
        setSubtitle(model.subtitleIcon, model.subtitle)
    }

    fun setFiatAmount(priceAmount: String?) {
        swapAmountInputFiat.setTextOrHide(priceAmount)
    }

    class Model(
        val assetImageUrl: String?,
        val title: String,
        val subtitleIcon: Icon?,
        val subtitle: String
    )
}
