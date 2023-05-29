package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.userRewards

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.address
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
        chain: Chain,
        chainAsset: Chain.Asset,
        rewardPeriod: RewardPeriod
    ): Result<*> = withContext(Dispatchers.Default) {
        runCatching {
            stakingRewardsRepository.sync(delegator.address(), chain, chainAsset, rewardPeriod)
        }
    }

    fun observeRewards(
        delegator: DelegatorState.Delegator,
        chain: Chain,
        chainAsset: Chain.Asset
    ) = flow {
        val rewardsFlow = stakingRewardsRepository.totalRewardFlow(delegator.address(), chain.id, chainAsset.id)

        emitAll(rewardsFlow)
    }
}
