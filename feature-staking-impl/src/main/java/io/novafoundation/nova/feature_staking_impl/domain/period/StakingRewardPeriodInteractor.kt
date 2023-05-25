package io.novafoundation.nova.feature_staking_impl.domain.period

import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import kotlinx.coroutines.flow.Flow

interface StakingRewardPeriodInteractor {

    suspend fun setRewardPeriod(rewardPeriod: RewardPeriod)

    fun getRewardPeriod(): RewardPeriod

    fun observeRewardPeriod(): Flow<RewardPeriod>
}

class RealStakingRewardPeriodInteractor(
    private val stakingPeriodRepository: StakingPeriodRepository,
    private val stakingRewardsRepository: StakingRewardsRepository
) : StakingRewardPeriodInteractor {

    override suspend fun setRewardPeriod(rewardPeriod: RewardPeriod) {
        stakingRewardsRepository.clearRewards()
        stakingPeriodRepository.setRewardPeriod(rewardPeriod)
    }

    override fun getRewardPeriod(): RewardPeriod {
        return stakingPeriodRepository.getRewardPeriod()
    }

    override fun observeRewardPeriod(): Flow<RewardPeriod> {
        return stakingPeriodRepository.observeRewardPeriod()
    }
}
