package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.feature_staking_impl.presentation.view.RewardDestinationView
import io.novafoundation.nova.feature_staking_impl.presentation.view.showRewardEstimation

fun BaseFragmentMixin<*>.setupParachainStakingRewardsComponent(
    component: StakingRewardEstimationComponent,
    view: RewardDestinationView
) {
    component.rewardEstimation.observe(view::showRewardEstimation)
}
