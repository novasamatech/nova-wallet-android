package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeGoneViews
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.makeVisibleViews
import io.novafoundation.nova.feature_staking_impl.databinding.ViewEstimatedRewardsBinding

class EstimatedRewardsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewEstimatedRewardsBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun showLoading() {
        makeGoneViews(binder.viewEstimatedRewardsEarnings, binder.viewEstimatedRewardsEarningsSuffix)
        binder.viewEstimatedRewardsEarningsShimmer.makeVisible()
    }

    fun showEarnings(earnings: String) {
        makeVisibleViews(binder.viewEstimatedRewardsEarnings, binder.viewEstimatedRewardsEarningsSuffix)
        binder.viewEstimatedRewardsEarningsShimmer.makeGone()

        binder.viewEstimatedRewardsEarnings.text = earnings
    }
}
