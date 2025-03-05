package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.loadHasStakingComponentState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

context(CoroutineScope)
fun <T> NominationPoolSharedComputation.loadPoolMemberState(
    hostContext: ComponentHostContext,
    chain: Chain,
    stateProducer: suspend (PoolMember) -> Flow<T>,
    distinctUntilChanged: (PoolMember?, PoolMember?) -> Boolean = { _, _ -> false },
): Flow<LoadingState<T>?> = loadHasStakingComponentState(
    hostContext = hostContext,
    hasStakingStateProducer = { currentPoolMemberFlow(chain, this@CoroutineScope).distinctUntilChanged(distinctUntilChanged) },
    componentStateProducer = stateProducer,
    onComponentStateChange = {}
)
