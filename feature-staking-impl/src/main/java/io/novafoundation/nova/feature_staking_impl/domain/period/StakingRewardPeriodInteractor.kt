package io.novafoundation.nova.feature_staking_impl.domain.period

import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import kotlinx.coroutines.flow.Flow

interface StakingRewardPeriodInteractor {

    fun setRewardPeriod(rewardPeriod: RewardPeriod)

    fun getRewardPeriod(): RewardPeriod

    fun getRewardPeriodFlow(): Flow<RewardPeriod>
}

class RealStakingRewardPeriodInteractor(
    private val stakingPeriodRepository: StakingPeriodRepository
) : StakingRewardPeriodInteractor {

    override fun setRewardPeriod(rewardPeriod: RewardPeriod) {
        stakingPeriodRepository.setRewardPeriod(rewardPeriod)
    }

    override fun getRewardPeriod(): RewardPeriod {
        return stakingPeriodRepository.getRewardPeriod()
    }

    override fun getRewardPeriodFlow(): Flow<RewardPeriod> {
        return stakingPeriodRepository.getRewardPeriodFlow()
    }
}
