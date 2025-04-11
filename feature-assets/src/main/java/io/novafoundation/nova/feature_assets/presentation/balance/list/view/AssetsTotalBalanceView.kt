package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setShimmerVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.parallaxCard.ParallaxCardView
import io.novafoundation.nova.feature_assets.databinding.ViewTotalBalanceBinding
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel

class AssetsTotalBalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ParallaxCardView(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    private val binder = ViewTotalBalanceBinding.inflate(inflater(), this)

    init {
        setPadding(
            12.dp(context),
            0,
            12.dp(context),
            12.dp(context)
        )
    }

    fun showTotalBalance(totalBalance: TotalBalanceModel) {
        binder.viewAssetsTotalBalanceShimmer.setShimmerVisible(false)
        binder.viewAssetsTotalBalanceTotal.setVisible(true)
        binder.viewAssetsTotalBalanceTotal.text = totalBalance.totalBalanceFiat
        binder.viewAssetsTotalBalanceTotal.requestLayout() // to fix the issue when elipsing the text is working incorrectly during fast text update

        binder.viewAssetsTotalBalanceLockedContainer.setVisible(totalBalance.isBreakdownAbailable)

        binder.viewAssetsTotalBalanceLocked.text = totalBalance.lockedBalanceFiat
        binder.viewAssetsTotalBalanceSwap.isEnabled = totalBalance.enableSwap
    }

    fun onSendClick(clickListener: OnClickListener) {
        binder.viewAssetsTotalBalanceSend.setOnClickListener(clickListener)
    }

    fun onReceiveClick(clickListener: OnClickListener) {
        binder.viewAssetsTotalBalanceReceive.setOnClickListener(clickListener)
    }

    fun onSwapClick(clickListener: OnClickListener) {
        binder.viewAssetsTotalBalanceSwap.setOnClickListener(clickListener)
    }

    fun onBuyClick(clickListener: OnClickListener) {
        binder.viewAssetsTotalBalanceBuy.setOnClickListener(clickListener)
    }
}
