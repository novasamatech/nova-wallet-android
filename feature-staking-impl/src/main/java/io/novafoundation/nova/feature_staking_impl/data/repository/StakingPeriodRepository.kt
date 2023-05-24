package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingRewardPeriodDataSource
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import kotlinx.coroutines.flow.Flow

interface StakingPeriodRepository {

    fun setRewardPeriod(rewardPeriod: RewardPeriod)

    fun getRewardPeriod(): RewardPeriod

    fun observeRewardPeriod(): Flow<RewardPeriod>
}

class RealStakingPeriodRepository(
    private val stakingRewardPeriodDataSource: StakingRewardPeriodDataSource
) : StakingPeriodRepository {

    override fun setRewardPeriod(rewardPeriod: RewardPeriod) {
        stakingRewardPeriodDataSource.setRewardPeriod(rewardPeriod)
    }

    override fun getRewardPeriod(): RewardPeriod {
        return stakingRewardPeriodDataSource.getRewardPeriod()
    }

    override fun observeRewardPeriod(): Flow<RewardPeriod> {
        return stakingRewardPeriodDataSource.observeRewardPeriod()
    }
}
