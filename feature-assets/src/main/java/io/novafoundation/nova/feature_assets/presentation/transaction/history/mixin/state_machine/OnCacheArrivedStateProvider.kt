package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine

import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.Action

object OnCacheArrivedStateProvider : TransactionStateMachine.StateProvider<Action.CachePageArrived> {

    override fun getState(
        action: Action.CachePageArrived,
        state: TransactionStateMachine.State,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit
    ): TransactionStateMachine.State {
        val nextOffset = action.newPage.nextOffset

        return when {
            !canUseCache(state.allAvailableFilters, state.usedFilters) -> stateCanNotUseCache(action, sideEffectListener, state)

            nextOffset is PageOffset.Loadable -> statePageIsLoadable(action, sideEffectListener, nextOffset, state)

            action.newPage.isEmpty() -> TransactionStateMachine.State.Empty(
                allAvailableFilters = state.allAvailableFilters,
                usedFilters = state.usedFilters
            )

            else -> TransactionStateMachine.State.FullData(
                data = action.newPage,
                allAvailableFilters = state.allAvailableFilters,
                usedFilters = state.usedFilters
            )
        }
    }

    private fun stateCanNotUseCache(
        action: Action.CachePageArrived,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit,
        state: TransactionStateMachine.State
    ) = if (action.accountChanged) {
        // trigger cold load for new account when not able to use cache
        sideEffectListener(TransactionStateMachine.SideEffect.LoadPage(nextPageOffset = PageOffset.Loadable.FirstPage, filters = state.usedFilters))

        TransactionStateMachine.State.EmptyProgress(
            allAvailableFilters = state.allAvailableFilters,
            usedFilters = state.usedFilters
        )
    } else {
        // if account is the same - ignore new page, since cache is not used
        state
    }

    private fun statePageIsLoadable(
        action: Action.CachePageArrived,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit,
        nextOffset: PageOffset.Loadable,
        state: TransactionStateMachine.State
    ) = if (action.newPage.size < TransactionStateMachine.PAGE_SIZE) {
        // cache page doesn't have enough items but we can load them
        sideEffectListener(TransactionStateMachine.SideEffect.LoadPage(nextPageOffset = nextOffset, state.usedFilters))

        if (action.newPage.isEmpty()) {
            TransactionStateMachine.State.EmptyProgress(state.allAvailableFilters, state.usedFilters)
        } else {
            TransactionStateMachine.State.NewPageProgress(nextOffset, action.newPage, state.allAvailableFilters, state.usedFilters)
        }
    } else {
        // cache page has enough items so we wont load next page automatically
        if (action.newPage.isEmpty()) {
            TransactionStateMachine.State.Empty(state.allAvailableFilters, state.usedFilters)
        } else {
            TransactionStateMachine.State.Data(nextOffset, action.newPage, state.allAvailableFilters, state.usedFilters)
        }
    }
}
