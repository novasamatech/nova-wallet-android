package io.novafoundation.nova.feature_staking_impl.domain.mythos.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.data.memory.SharedComputation
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.mythos.duration.MythosSessionDurationCalculator
import io.novafoundation.nova.feature_staking_impl.data.mythos.duration.MythosSessionDurationCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_staking_impl.data.repository.SessionRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@FeatureScope
class MythosSharedComputation @Inject constructor(
    private val userStakeRepository: MythosUserStakeUseCase,
    private val mythosStakingRepository: MythosStakingRepository,
    private val sessionRepository: SessionRepository,
    private val mythosSessionDurationCalculatorFactory: MythosSessionDurationCalculatorFactory,
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
    fun userStakeFlow(chain: Chain): Flow<UserStakeInfo> {
        return cachedFlow("MythosSharedComputation.userStakeFlow", chain.id) {
            userStakeRepository.currentUserStakeInfo(chain)
        }
    }

    context(ComputationalScope)
    fun sessionValidatorsFlow(chainId: ChainId): Flow<SessionValidators> {
        return cachedFlow("MythosSharedComputation.sessionValidatorsFlow", chainId) {
            sessionRepository.sessionValidatorsFlow(chainId)
        }
    }
}
