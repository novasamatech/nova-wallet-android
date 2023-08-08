package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.scope

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.coroutines.coroutineContext

class PoolScope(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val stakingSharedState: StakingSharedState,
) : UpdateScope<PoolId?> {

    override fun invalidationFlow(): Flow<PoolId?> {
        return poolMemberFlow()
            .map { it?.poolId }
            .distinctUntilChanged()
    }

    private fun poolMemberFlow() = flowOfAll {
        val scope = CoroutineScope(coroutineContext)
        val chain = stakingSharedState.chain()

        nominationPoolSharedComputation.currentPoolMemberFlow(chain, scope)
    }
}
