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
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_swap_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class SwapAssetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(
        context
    ) {

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        inflate(context, R.layout.view_swap_asset, this)
        background = context.getInputBackground()
    }

    fun setModel(model: Model) {
        setAssetImageUrl(model.assetIcon)
        setAmount(model.amount)
        setNetwork(model.chainUi)
        swapAssetAmount.setTextColorRes(model.amountTextColorRes)
    }

    private fun setAssetImageUrl(icon: Icon) {
        swapAssetImage.setImageTint(context.getColor(R.color.icon_primary))
        swapAssetImage.setIcon(icon, imageLoader)
        swapAssetImage.setBackgroundResource(R.drawable.bg_token_container)
    }

    private fun setAmount(amount: AmountModel) {
        swapAssetAmount.text = amount.token
        swapAssetFiat.setTextOrHide(amount.fiat)
    }

    private fun setNetwork(chainUi: ChainUi) {
        swapAssetNetworkImage.loadChainIcon(chainUi.icon, imageLoader)
        swapAssetNetwork.text = chainUi.name
    }

    class Model(
        val assetIcon: Icon,
        val amount: AmountModel,
        val chainUi: ChainUi,
        @ColorRes val amountTextColorRes: Int = R.color.text_primary
    )
}
