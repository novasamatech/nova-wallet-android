package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards

import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation
import kotlinx.coroutines.flow.Flow

interface StakingRewardEstimationComponent {

    val rewardEstimation: Flow<RewardEstimation>
}
