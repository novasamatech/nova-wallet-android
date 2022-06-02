package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking

import android.util.Log
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.withIndex

fun <T> DelegatorStateUseCase.loadDelegatingState(
    hostContext: ComponentHostContext,
    assetWithChain: SingleAssetSharedState.AssetWithChain,
    stateProducer: suspend (DelegatorState.Delegator) -> Flow<T>,
    onDelegatorChange: (DelegatorState.Delegator) -> Unit = {}
): Flow<LoadingState<T>?> = hostContext.selectedAccount.transformLatest { account ->
    emit(null) // hide UI until state of delegator is determined

    val stateFlow = delegatorStateFlow(account, assetWithChain.chain, assetWithChain.asset)
        .withIndex()
        .transformLatest { (index, delegatorState) ->
            if (delegatorState is DelegatorState.Delegator) {
                // first loading of might take a while - show loading.
                // We do not show loading for subsequent updates since there is already some info on the screen from the first load
                if (index == 0) {
                    onDelegatorChange(delegatorState)

                    emit(LoadingState.Loading())
                }

                val summaryFlow = stateProducer(delegatorState).map { LoadingState.Loaded(it) }

                emitAll(summaryFlow)
            } else {
                emit(null)
            }
        }

    emitAll(stateFlow)
}
    .onStart { emit(null) }
    .catch { Log.e("StatefullComponent", "Failed to construct state", it) }
