package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine

import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.Action
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.State

object OnPageErrorStateProvider : TransactionStateMachine.StateProvider<Action.PageError> {

    override fun getState(
        action: Action.PageError,
        state: State,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit
    ): State {
        sideEffectListener(TransactionStateMachine.SideEffect.ErrorEvent(action.error))

        return when (state) {
            is State.EmptyProgress -> State.Empty(
                allAvailableFilters = state.allAvailableFilters,
                usedFilters = state.usedFilters
            )
            is State.NewPageProgress -> State.Data(
                nextPageOffset = state.nextPageOffset,
                data = state.data,
                allAvailableFilters = state.allAvailableFilters,
                usedFilters = state.usedFilters
            )
            else -> state
        }
    }
}
