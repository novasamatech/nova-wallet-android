package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.userRewards

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.address
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ParachainStakingUserRewardsInteractor(
    private val stakingRewardsRepository: StakingRewardsRepository
) {

    suspend fun syncRewards(delegator: DelegatorState.Delegator): Result<*> = withContext(Dispatchers.Default) {
        runCatching {
            stakingRewardsRepository.sync(delegator.address(), delegator.chain)
        }
    }

    fun observeRewards(delegator: DelegatorState.Delegator) = flow {
        emitAll(stakingRewardsRepository.totalRewardFlow(delegator.address()))
    }
}
