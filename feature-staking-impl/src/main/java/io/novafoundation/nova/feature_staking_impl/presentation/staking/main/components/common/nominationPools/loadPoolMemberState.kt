package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.loadHasStakingComponentState
import kotlinx.coroutines.flow.Flow

fun <T> NominationPoolMemberUseCase.loadPoolMemberState(
    hostContext: ComponentHostContext,
    stateProducer: suspend (PoolMember) -> Flow<T>,
    onPoolMemberChange: (PoolMember) -> Unit = {}
): Flow<LoadingState<T>?> = loadHasStakingComponentState(
    hostContext = hostContext,
    hasStakingStateProducer = { currentPoolMemberFlow() },
    componentStateProducer = stateProducer,
    onComponentStateChange = onPoolMemberChange
)
