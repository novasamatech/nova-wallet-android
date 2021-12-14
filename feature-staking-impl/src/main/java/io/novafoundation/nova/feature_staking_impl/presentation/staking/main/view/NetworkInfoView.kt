package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount
import kotlinx.android.synthetic.main.view_network_info.view.stakingAboutActiveNominators
import kotlinx.android.synthetic.main.view_network_info.view.stakingAboutMinimumStake
import kotlinx.android.synthetic.main.view_network_info.view.stakingAboutStakingPeriod
import kotlinx.android.synthetic.main.view_network_info.view.stakingAboutTotalStake
import kotlinx.android.synthetic.main.view_network_info.view.stakingAboutUnstakingPeriod
import kotlinx.android.synthetic.main.view_network_info.view.stakingNetworkCollapsibleView
import kotlinx.android.synthetic.main.view_network_info.view.stakingNetworkInfoTitle

private const val ANIMATION_DURATION = 220L

class NetworkInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    enum class State {
        EXPANDED,
        COLLAPSED
    }

    private var currentState = State.COLLAPSED

    init {
        View.inflate(context, R.layout.view_network_info, this)

        with(context) {
            background = getBlurDrawable()
        }

        orientation = VERTICAL

        stakingNetworkInfoTitle.setOnClickListener { changeExpandableState() }
    }

    fun setTitle(title: String) {
        stakingNetworkInfoTitle.text = title
    }

    fun showLoading() {
        stakingAboutTotalStake.showProgress()
        stakingAboutMinimumStake.showProgress()
        stakingAboutActiveNominators.showProgress()
        stakingAboutStakingPeriod.showProgress()
        stakingAboutUnstakingPeriod.showProgress()
    }

    fun setTotalStaked(amountModel: AmountModel) {
        stakingAboutTotalStake.showAmount(amountModel)
    }

    fun setMinimumStake(amountModel: AmountModel) {
        stakingAboutMinimumStake.showAmount(amountModel)
    }

    fun setNominatorsCount(nominatorsCount: String) {
        stakingAboutActiveNominators.showValue(nominatorsCount)
    }

    fun setStakingPeriod(period: String) {
        stakingAboutStakingPeriod.showValue(period)
    }

    fun setUnstakingPeriod(period: String) {
        stakingAboutUnstakingPeriod.showValue(period)
    }

    fun setExpanded(expanded: Boolean) {
        if (expanded) {
            expand()
        } else {
            collapse()
        }
    }

    private fun changeExpandableState() {
        if (State.EXPANDED == currentState) {
            collapse()
        } else {
            expand()
        }
    }

    private fun collapse() {
        if (currentState == State.COLLAPSED) return

        stakingNetworkInfoTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_down, 0)
        currentState = State.COLLAPSED
        stakingNetworkCollapsibleView.animate()
            .setDuration(ANIMATION_DURATION)
            .alpha(0f)
            .withEndAction { stakingNetworkCollapsibleView.makeGone() }
    }

    private fun expand() {
        if (currentState == State.EXPANDED) return

        stakingNetworkInfoTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_up, 0)
        stakingNetworkCollapsibleView.makeVisible()
        currentState = State.EXPANDED
        stakingNetworkCollapsibleView.animate()
            .setDuration(ANIMATION_DURATION)
            .alpha(1f)
    }
}
