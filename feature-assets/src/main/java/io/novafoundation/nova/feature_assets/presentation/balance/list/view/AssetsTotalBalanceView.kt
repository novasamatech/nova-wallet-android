package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setShimmerVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.parallaxCard.ParallaxCardView
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceBuy
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceLocked
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceLockedContainer
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceReceive
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceSend
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceShimmer
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceSwap
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceTotal

class AssetsTotalBalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ParallaxCardView(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_total_balance, this)

        setPadding(
            12.dp(context),
            0,
            12.dp(context),
            12.dp(context)
        )
    }

    fun showTotalBalance(totalBalance: TotalBalanceModel) {
        viewAssetsTotalBalanceShimmer.setShimmerVisible(false)
        viewAssetsTotalBalanceTotal.setVisible(true)
        viewAssetsTotalBalanceTotal.text = totalBalance.totalBalanceFiat
        viewAssetsTotalBalanceTotal.requestLayout() // to fix the issue when elipsing the text is working incorrectly during fast text update

        viewAssetsTotalBalanceLockedContainer.setVisible(totalBalance.isBreakdownAbailable)

        viewAssetsTotalBalanceLocked.text = totalBalance.lockedBalanceFiat
        viewAssetsTotalBalanceSwap.isEnabled = totalBalance.enableSwap
    }

    fun onSendClick(clickListener: OnClickListener) {
        viewAssetsTotalBalanceSend.setOnClickListener(clickListener)
    }

    fun onReceiveClick(clickListener: OnClickListener) {
        viewAssetsTotalBalanceReceive.setOnClickListener(clickListener)
    }

    fun onSwapClick(clickListener: OnClickListener) {
        viewAssetsTotalBalanceSwap.setOnClickListener(clickListener)
    }

    fun onBuyClick(clickListener: OnClickListener) {
        viewAssetsTotalBalanceBuy.setOnClickListener(clickListener)
    }
}
