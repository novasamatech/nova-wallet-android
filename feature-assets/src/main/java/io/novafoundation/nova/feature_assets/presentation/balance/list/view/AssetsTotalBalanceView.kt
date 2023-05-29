package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setShimmerVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.paralaxCard.ParalaxCardView
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceLocked
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceShimmer
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceTitle
import kotlinx.android.synthetic.main.view_total_balance.view.viewAssetsTotalBalanceTotal

class AssetsTotalBalanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ParalaxCardView(context, attrs, defStyleAttr), WithContextExtensions {

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
        viewAssetsTotalBalanceTotal.setVisible(true, falseState = View.INVISIBLE)
        viewAssetsTotalBalanceTotal.text = totalBalance.totalBalanceFiat

        viewAssetsTotalBalanceLocked.setVisible(totalBalance.isBreakdownAbailable)
        viewAssetsTotalBalanceLocked.text = totalBalance.lockedBalanceFiat
    }
}
