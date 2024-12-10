package io.novafoundation.nova.feature_swap_impl.presentation.route.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.ViewTokenAmountBinding
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class TokenAmountModel(
    val amount: String,
    val tokenIcon: Icon
) {

    companion object {

        fun from(chainAsset: Chain.Asset, assetIcon: Icon, amount: Balance): TokenAmountModel {
            return TokenAmountModel(
                amount = amount.formatPlanks(chainAsset),
                tokenIcon = assetIcon
            )
        }
    }
}

class TokenAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binder = ViewTokenAmountBinding.inflate(inflater(), this)

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun setModel(model: TokenAmountModel) {
        binder.viewTokenAmountAmount.text = model.amount
        binder.viewTokenAmountIcon.setIcon(model.tokenIcon, imageLoader)
    }
}
