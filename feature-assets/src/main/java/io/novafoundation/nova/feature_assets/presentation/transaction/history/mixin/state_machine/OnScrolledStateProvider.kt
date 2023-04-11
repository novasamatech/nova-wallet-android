package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine

import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.Action
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.PAGE_SIZE
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.State

object OnScrolledStateProvider : TransactionStateMachine.StateProvider<Action.Scrolled> {

    private const val SCROLL_OFFSET = PAGE_SIZE / 2

    override fun getState(
        action: Action.Scrolled,
        state: State,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit
    ): State = when (state) {
        is State.Data -> {
            if (action.currentItemIndex >= state.data.size - SCROLL_OFFSET) {
                sideEffectListener(TransactionStateMachine.SideEffect.LoadPage(state.nextPageOffset, state.usedFilters))

                State.NewPageProgress(
                    nextPageOffset = state.nextPageOffset,
                    data = state.data,
                    allAvailableFilters = state.allAvailableFilters,
                    usedFilters = state.usedFilters
                )
            } else {
                state
            }
        }

        else -> state
    }
}
