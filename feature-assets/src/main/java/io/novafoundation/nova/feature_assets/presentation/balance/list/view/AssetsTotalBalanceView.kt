package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setShimmerVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceLocked
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceShimmer
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceTotal

class AssetsTotalBalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_total_balance, this)

        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        viewAssetsTotalBalanceLocked.background = addRipple(
            getRoundedCornerDrawable(
                fillColorRes = R.color.white_16,
                cornerSizeDp = 6
            )
        )

        background = addRipple(getRoundedCornerDrawable(R.color.black_48))
    }

    fun showTotalBalance(totalBalance: TotalBalanceModel) {
        viewAssetsTotalBalanceShimmer.setShimmerVisible(false)

        viewAssetsTotalBalanceTotal.setVisible(true)
        viewAssetsTotalBalanceTotal.text = totalBalance.totalBalanceFiat

        viewAssetsTotalBalanceLocked.setVisible(true)
        viewAssetsTotalBalanceLocked.text = totalBalance.lockedBalanceFiat
    }
}
