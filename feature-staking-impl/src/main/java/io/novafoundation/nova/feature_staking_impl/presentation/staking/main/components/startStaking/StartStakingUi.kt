package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingRewardEstimationBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view.EstimateEarningView
import kotlinx.android.synthetic.main.fragment_staking.stakingEstimate

fun BaseFragment<*>.setupStartStakingComponent(component: StartStakingComponent, view: EstimateEarningView) {
    // state
    component.state.observe { startStakingState ->
        if (startStakingState == null) {
            view.makeGone()
            return@observe
        }

        view.makeVisible()

        view.setTitle(startStakingState.estimateEarningsTitle)

        when (val returnsState = startStakingState.returns) {
            is LoadingState.Loaded -> {
                val rewards = returnsState.data

                stakingEstimate.showGains(rewards.monthlyPercentage, rewards.yearlyPercentage)
            }

            is LoadingState.Loading -> stakingEstimate.showLoading()
        }
    }

    // actions
    stakingEstimate.startStakingButton.setOnClickListener {
        component.onAction(StartStakingAction.NextClicked)
    }

    stakingEstimate.infoActions.setOnClickListener {
        component.onAction(StartStakingAction.InfoClicked)
    }

    // events
    component.events.observeEvent {
        when (it) {
            is StartStakingEvent.ShowRewardEstimationDetails -> {
                StakingRewardEstimationBottomSheet(requireContext(), it.payload).show()
            }
        }
    }
}
