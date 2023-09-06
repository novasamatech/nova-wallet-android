package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.userRewards

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.fullId
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ParachainStakingUserRewardsInteractor(
    private val stakingRewardsRepository: StakingRewardsRepository,
    private val stakingPeriodRepository: StakingPeriodRepository
) {

    suspend fun syncRewards(
        delegator: DelegatorState.Delegator,
        stakingOption: StakingOption,
        rewardPeriod: RewardPeriod
    ): Result<*> = withContext(Dispatchers.Default) {
        runCatching {
            stakingRewardsRepository.sync(delegator.accountId, stakingOption, rewardPeriod)
        }.onFailure {
            Log.e(this@ParachainStakingUserRewardsInteractor.LOG_TAG, "Failed to sync rewards: $it")
        }
    }

    fun observeRewards(
        delegator: DelegatorState.Delegator,
        stakingOption: StakingOption,
    ) = flow {
        val rewardsFlow = stakingRewardsRepository.totalRewardFlow(delegator.accountId, stakingOption.fullId)

        emitAll(rewardsFlow)
    }
}
