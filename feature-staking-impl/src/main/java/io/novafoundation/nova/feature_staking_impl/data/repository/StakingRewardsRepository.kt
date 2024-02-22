package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.fullId
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward.StakingRewardsDataSourceRegistry
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface StakingRewardsRepository {

    fun totalRewardFlow(accountId: AccountId, stakingOptionId: StakingOptionId): Flow<TotalReward>

    suspend fun sync(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod)
}

class RealStakingRewardsRepository(
    private val dataSourceRegistry: StakingRewardsDataSourceRegistry,
) : StakingRewardsRepository {

    override fun totalRewardFlow(accountId: AccountId, stakingOptionId: StakingOptionId): Flow<TotalReward> {
        return sourceFor(stakingOptionId).totalRewardsFlow(accountId, stakingOptionId)
    }

    override suspend fun sync(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod) {
        return sourceFor(stakingOption.fullId).sync(accountId, stakingOption, rewardPeriod)
    }

    private fun sourceFor(stakingOption: StakingOptionId) = dataSourceRegistry.getDataSourceFor(stakingOption)
}
