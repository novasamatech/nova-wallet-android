package io.novafoundation.nova.feature_swap_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_swap_api.R
import io.novafoundation.nova.feature_swap_api.databinding.ViewSwapAssetBinding
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
        setAmount(model.amount)
        setNetwork(model.chainUi)
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

    private fun setNetwork(chainUi: ChainUi) {
        binder.swapAssetNetworkImage.loadChainIcon(chainUi.icon, imageLoader)
        binder.swapAssetNetwork.text = chainUi.name
    }

    class Model(
        val assetIcon: Icon,
        val amount: AmountModel,
        val chainUi: ChainUi,
        @ColorRes val amountTextColorRes: Int = R.color.text_primary
    )
}
