package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope

import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import kotlinx.coroutines.flow.Flow

class ActiveEraScope(
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingSharedState: StakingSharedState,
): UpdateScope<EraIndex> {

    override fun invalidationFlow(): Flow<EraIndex?> {
       return withFlowScope { flowScope ->
           val chainId = stakingSharedState.chainId()

           stakingSharedComputation.activeEraFlow(chainId, flowScope)
       }
    }
}
