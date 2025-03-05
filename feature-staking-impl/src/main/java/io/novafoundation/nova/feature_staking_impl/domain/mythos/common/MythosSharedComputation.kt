package io.novafoundation.nova.feature_staking_impl.domain.mythos.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.toHex
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.data.memory.SharedComputation
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.mythos.duration.MythosSessionDurationCalculator
import io.novafoundation.nova.feature_staking_impl.data.mythos.duration.MythosSessionDurationCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythCandidateInfos
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythReleaseRequest
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosCandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_staking_impl.data.repository.SessionRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.rewards.MythosStakingRewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.rewards.MythosStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@FeatureScope
class MythosSharedComputation @Inject constructor(
    private val mythosDelegatorStateUseCase: MythosDelegatorStateUseCase,
    private val mythosStakingRepository: MythosStakingRepository,
    private val sessionRepository: SessionRepository,
    private val mythosSessionDurationCalculatorFactory: MythosSessionDurationCalculatorFactory,
    private val candidatesRepository: MythosCandidatesRepository,
    private val rewardCalculatorFactory: MythosStakingRewardCalculatorFactory,
    private val userStakeRepository: MythosUserStakeRepository,
    computationalCache: ComputationalCache
) : SharedComputation(computationalCache) {

    context(ComputationalScope)
    fun eraDurationCalculatorFlow(stakingOption: StakingOption): Flow<MythosSessionDurationCalculator> {
        return cachedFlow("MythosSharedComputation.eraDurationCalculatorFlow", stakingOption.chain.id) {
            mythosSessionDurationCalculatorFactory.create(stakingOption)
        }
    }

    context(ComputationalScope)
    fun minStakeFlow(chainId: ChainId): Flow<Balance> {
        return cachedFlow("MythosSharedComputation.minStakeFlow", chainId) {
            mythosStakingRepository.minStakeFlow(chainId)
        }
    }

    context(ComputationalScope)
    fun delegatorStateFlow(): Flow<MythosDelegatorState> {
        return cachedFlow("MythosSharedComputation.userStakeFlow") {
            mythosDelegatorStateUseCase.currentDelegatorState()
        }
    }

    context(ComputationalScope)
    fun sessionValidatorsFlow(chainId: ChainId): Flow<SessionValidators> {
        return cachedFlow("MythosSharedComputation.sessionValidatorsFlow", chainId) {
            sessionRepository.sessionValidatorsFlow(chainId)
        }
    }

    context(ComputationalScope)
    suspend fun candidateInfos(chainId: ChainId): MythCandidateInfos {
        return cachedValue("MythosSharedComputation.candidateInfos", chainId) {
            candidatesRepository.getCandidateInfos(chainId)
        }
    }

    context(ComputationalScope)
    suspend fun rewardCalculator(chainId: ChainId): MythosStakingRewardCalculator {
        return cachedValue("MythosSharedComputation.rewardCalculator", chainId) {
            rewardCalculatorFactory.create(chainId)
        }
    }

    context(ComputationalScope)
    fun releaseQueuesFlow(chainId: ChainId, accountId: AccountIdKey): Flow<List<MythReleaseRequest>> {
        return cachedFlow("MythosSharedComputation.releaseQueuesFlow", chainId, accountId.toHex()) {
            userStakeRepository.releaseQueuesFlow(chainId, accountId)
        }
    }
}

context(ComputationalScope)
suspend fun MythosSharedComputation.sessionValidators(chainId: ChainId): SessionValidators {
    return sessionValidatorsFlow(chainId).first()
}

context(ComputationalScope)
suspend fun MythosSharedComputation.delegatorState(): MythosDelegatorState {
    return delegatorStateFlow().first()
}

context(ComputationalScope)
suspend fun MythosSharedComputation.minStake(chainId: ChainId): Balance {
    return minStakeFlow(chainId).first()
}
