package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.mythos

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.loadHasStakingComponentState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

fun <T> MythosSharedComputation.loadUserStakeState(
    hostContext: ComponentHostContext,
    chain: Chain,
    stateProducer: suspend (MythosDelegatorState) -> Flow<T>,
    distinctUntilChanged: (MythosDelegatorState, MythosDelegatorState) -> Boolean = { _, _ -> false },
): Flow<LoadingState<T>?> = loadHasStakingComponentState(
    hostContext = hostContext,
    hasStakingStateProducer = {
        with(hostContext.scope) {
            delegatorStateFlow().distinctUntilChanged(distinctUntilChanged)
        }
    },
    componentStateProducer = stateProducer,
    onComponentStateChange = {}
)
