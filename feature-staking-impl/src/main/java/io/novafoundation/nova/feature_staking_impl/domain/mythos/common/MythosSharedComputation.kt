package io.novafoundation.nova.feature_staking_impl.domain.mythos.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.data.memory.SharedComputation
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_staking_impl.data.repository.SessionRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@FeatureScope
class MythosSharedComputation @Inject constructor(
    private val userStakeRepository: MythosUserStakeUseCase,
    private val sessionRepository: SessionRepository,
    computationalCache: ComputationalCache
) : SharedComputation(computationalCache) {

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
