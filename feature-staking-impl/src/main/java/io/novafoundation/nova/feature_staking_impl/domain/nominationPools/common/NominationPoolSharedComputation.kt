package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class NominationPoolSharedComputation(
    private val computationalCache: ComputationalCache,
    private val nominationPoolMemberUseCase: NominationPoolMemberUseCase
) {

    fun currentPoolMemberFlow(chain: Chain, scope: CoroutineScope): Flow<PoolMember?> {
        val key = "POOL_MEMBER:${chain.id}"

        return computationalCache.useSharedFlow(key, scope) {
            nominationPoolMemberUseCase.currentPoolMemberFlow(chain)
        }
    }
}
