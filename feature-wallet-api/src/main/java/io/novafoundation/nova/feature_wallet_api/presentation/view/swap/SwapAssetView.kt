package io.novafoundation.nova.feature_wallet_api.presentation.view.swap

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ViewSwapAssetBinding
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class SwapAssetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewSwapAssetBinding.inflate(inflater(), this)

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        background = context.getInputBackground()
    }

    fun setModel(model: Model) {
        setAssetImageUrl(model.assetIcon)
        binder.swapAssetImage.clipToOutline = model.clipIconToOutline
        setAmount(model.amount)
        setNetwork(model.chainUi)
        if (model.chainUi == null && model.amount.fiat.isNullOrEmpty()) {
            binder.swapAssetFiat.visibility = View.INVISIBLE
        }
        binder.swapAssetImage.updateLayoutParams {
            width = model.iconSizeDp.dp
            height = model.iconSizeDp.dp
        }
        binder.swapAssetAmount.setTextColorRes(model.amountTextColorRes)
    }

    private fun setAssetImageUrl(icon: Icon) {
        binder.swapAssetImage.setTokenIcon(icon, imageLoader)
        binder.swapAssetImage.setBackgroundResource(R.drawable.bg_token_container)
    }

    private fun setAmount(amount: AmountModel) {
        binder.swapAssetAmount.text = amount.token
        binder.swapAssetFiat.setTextOrHide(amount.fiat)
    }

    private fun setNetwork(chainUi: ChainUi?) {
        binder.swapAssetNetworkContainer.isVisible = chainUi != null
        binder.swapAssetFiat.updateLayoutParams<LayoutParams> {
            bottomToBottom = if (chainUi == null) LayoutParams.PARENT_ID else LayoutParams.UNSET
            bottomMargin = if (chainUi == null) 20.dp else 0
        }
        if (chainUi == null) return
        binder.swapAssetNetworkImage.loadChainIcon(chainUi.icon, imageLoader)
        binder.swapAssetNetwork.text = chainUi.name
    }

    class Model(
        val assetIcon: Icon,
        val amount: AmountModel,
        val chainUi: ChainUi? = null,
        @ColorRes val amountTextColorRes: Int = R.color.text_primary,
        val iconSizeDp: Int = 40,
        val clipIconToOutline: Boolean = false
    )
}
