package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface StakingRewardsDataSource {

    fun totalRewardsFlow(
        accountId: AccountId,
        stakingOptionId: StakingOptionId,
    ): Flow<TotalReward>

    suspend fun sync(
        accountId: AccountId,
        stakingOption: StakingOption,
        rewardPeriod: RewardPeriod
    )
}
