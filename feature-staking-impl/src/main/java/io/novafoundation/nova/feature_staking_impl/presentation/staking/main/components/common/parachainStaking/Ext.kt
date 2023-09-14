package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.loadHasStakingComponentState
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> DelegatorStateUseCase.loadDelegatingState(
    hostContext: ComponentHostContext,
    assetWithChain: ChainWithAsset,
    stateProducer: suspend (DelegatorState.Delegator) -> Flow<T>,
    onDelegatorChange: (DelegatorState.Delegator) -> Unit = {}
): Flow<LoadingState<T>?> = loadHasStakingComponentState(
    hostContext = hostContext,
    hasStakingStateProducer = { account ->
        delegatorStateFlow(account, assetWithChain.chain, assetWithChain.asset)
            .map { it as? DelegatorState.Delegator }
    },
    componentStateProducer = stateProducer,
    onComponentStateChange = onDelegatorChange
)
