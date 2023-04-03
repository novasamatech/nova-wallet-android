package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine

import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.Action

object OnFiltersChangedStateProvider : TransactionStateMachine.StateProvider<Action.FiltersChanged> {

    override fun getState(
        action: Action.FiltersChanged,
        state: TransactionStateMachine.State,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit
    ): TransactionStateMachine.State {
        val newFilters = action.newUsedFilters

        if (canUseCache(state.allAvailableFilters, newFilters)) {
            sideEffectListener(TransactionStateMachine.SideEffect.TriggerCache)
        } else {
            sideEffectListener(TransactionStateMachine.SideEffect.LoadPage(nextPageOffset = PageOffset.Loadable.FirstPage, filters = newFilters))
        }

        return TransactionStateMachine.State.EmptyProgress(
            allAvailableFilters = state.allAvailableFilters,
            usedFilters = newFilters
        )
    }
}
