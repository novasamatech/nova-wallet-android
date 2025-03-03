package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common

import android.util.Log
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.withIndex

inline fun <S, C> loadHasStakingComponentState(
    hostContext: ComponentHostContext,
    crossinline hasStakingStateProducer: (MetaAccount) -> Flow<S?>,
    crossinline componentStateProducer: suspend (S) -> Flow<C>,
    crossinline onComponentStateChange: (S) -> Unit
): Flow<LoadingState<C>?> = hostContext.selectedAccount.transformLatest { account ->
    emit(null) // hide UI until state of staking is determined

    val stateFlow = hasStakingStateProducer(account)
        .withIndex()
        .transformLatest { (index, hasStakingState) ->
            if (hasStakingState != null) {
                // first loading of might take a while - show loading.
                // We do not show loading for subsequent updates since there is already some info on the screen from the first load
                if (index == 0) {
                    onComponentStateChange(hasStakingState)

                    emit(LoadingState.Loading())
                }

                val summaryFlow = componentStateProducer(hasStakingState).map { LoadingState.Loaded(it) }

                emitAll(summaryFlow)
            } else {
                emit(null)
            }
        }

    emitAll(stateFlow)
}
    .onStart { emit(null) }
    .catch { Log.e("StatefullComponent", "Failed to construct state", it) }
