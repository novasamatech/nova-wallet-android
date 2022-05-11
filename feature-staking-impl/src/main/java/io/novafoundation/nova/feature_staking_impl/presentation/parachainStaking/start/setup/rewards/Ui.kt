package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.feature_staking_impl.presentation.view.RewardDestinationView
import io.novafoundation.nova.feature_staking_impl.presentation.view.showRewardEstimation
import kotlinx.coroutines.flow.filterNotNull

fun BaseFragmentMixin<*>.setupParachainStakingRewardsComponent(
    component: ParachainStakingRewardsComponent,
    view: RewardDestinationView
) {
    component.state.filterNotNull().observe {
        view.showRewardEstimation(it.rewardEstimation)
    }
}
