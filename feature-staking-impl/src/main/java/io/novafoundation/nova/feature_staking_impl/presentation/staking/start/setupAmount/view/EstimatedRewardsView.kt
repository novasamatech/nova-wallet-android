package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeGoneViews
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.makeVisibleViews
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_estimated_rewards.view.viewEstimatedRewardsEarnings
import kotlinx.android.synthetic.main.view_estimated_rewards.view.viewEstimatedRewardsEarningsShimmer
import kotlinx.android.synthetic.main.view_estimated_rewards.view.viewEstimatedRewardsEarningsSuffix

class EstimatedRewardsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_estimated_rewards, this)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun showLoading() {
        makeGoneViews(viewEstimatedRewardsEarnings, viewEstimatedRewardsEarningsSuffix)
        viewEstimatedRewardsEarningsShimmer.makeVisible()
    }

    fun showEarnings(earnings: String) {
        makeVisibleViews(viewEstimatedRewardsEarnings, viewEstimatedRewardsEarningsSuffix)
        viewEstimatedRewardsEarningsShimmer.makeGone()

        viewEstimatedRewardsEarnings.text = earnings
    }
}
