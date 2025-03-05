package io.novafoundation.nova.feature_staking_impl.domain.mythos.rewards

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.minStake
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.sessionValidators
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import javax.inject.Inject

@FeatureScope
class MythosStakingRewardCalculatorFactory @Inject constructor(
    private val mythosStakingRepository: MythosStakingRepository,
    private val chainStateRepository: ChainStateRepository,
    private val mythosSharedComputation: dagger.Lazy<MythosSharedComputation>,
) {

    context(ComputationalScope)
    suspend fun create(chainId: ChainId): MythosStakingRewardCalculator {
        val sessionValidators = mythosSharedComputation.get().sessionValidators(chainId).toSet()
        val candidateInfos = mythosSharedComputation.get().candidateInfos(chainId)
        val collators = candidateInfos
            .map { (accountId, candidateInfo) -> MythosStakingRewardTarget(candidateInfo.stake, accountId) }
            .filter { it.accountId in sessionValidators }

        return RealMythosStakingRewardCalculator(
            perBlockRewards = mythosStakingRepository.perBlockReward(chainId),
            blockDuration = chainStateRepository.expectedBlockTime(chainId),
            collatorCommission = mythosStakingRepository.collatorCommission(chainId),
            collators = collators,
            minStake = mythosSharedComputation.get().minStake(chainId)
        )
    }
}
