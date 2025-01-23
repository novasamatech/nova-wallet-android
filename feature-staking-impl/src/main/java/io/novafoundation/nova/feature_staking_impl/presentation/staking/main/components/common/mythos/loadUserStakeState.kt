package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.mythos

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.loadHasStakingComponentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> MythosSharedComputation.loadUserStakeState(
    hostContext: ComponentHostContext,
    stateProducer: suspend (MythosDelegatorState.Locked) -> Flow<T>,
): Flow<LoadingState<T>?> = loadHasStakingComponentState(
    hostContext = hostContext,
    hasStakingStateProducer = {
        with(hostContext.scope) {
            delegatorStateFlow()
                .map { it as? MythosDelegatorState.Locked }
        }
    },
    componentStateProducer = stateProducer,
    onComponentStateChange = {}
)
