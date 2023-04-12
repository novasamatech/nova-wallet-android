package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.Action
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.State
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation

object OnNewPageStateProvider : TransactionStateMachine.StateProvider<Action.NewPage> {

    override fun getState(
        action: Action.NewPage,
        state: State,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit
    ): State {
        val page = action.newPage
        val nextPageOffset = page.nextOffset

        return when (state) {
            is State.EmptyProgress -> onEmptyProgress(action, state, nextPageOffset, page, sideEffectListener)

            is State.NewPageProgress -> onNewPageProgress(nextPageOffset, state, page, sideEffectListener)

            else -> state
        }
    }

    private fun onEmptyProgress(
        action: Action.NewPage,
        state: State,
        nextPageOffset: PageOffset,
        page: DataPage<Operation>,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit
    ) = when {
        action.loadedWith != state.usedFilters -> state // not relevant anymore page has arrived, still loading

        nextPageOffset is PageOffset.FullData && page.isEmpty() -> State.Empty(
            allAvailableFilters = state.allAvailableFilters,
            usedFilters = state.usedFilters
        )

        nextPageOffset is PageOffset.FullData && page.isNotEmpty() -> State.FullData(
            data = page,
            allAvailableFilters = state.allAvailableFilters,
            usedFilters = state.usedFilters
        )

        nextPageOffset is PageOffset.Loadable -> {
            // we didn't load enough items but can load more -> trigger next page automatically
            if (page.items.size < TransactionStateMachine.PAGE_SIZE) {
                sideEffectListener(TransactionStateMachine.SideEffect.LoadPage(nextPageOffset, state.usedFilters))

                if (page.items.isEmpty()) {
                    State.EmptyProgress(
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                } else {
                    State.NewPageProgress(
                        nextPageOffset = nextPageOffset,
                        data = page,
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                }
            } else {
                State.Data(
                    nextPageOffset = nextPageOffset,
                    data = page,
                    allAvailableFilters = state.allAvailableFilters,
                    usedFilters = state.usedFilters
                )
            }
        }

        else -> error("Checked all cases of sealed class PageOffset QED")
    }

    private fun onNewPageProgress(
        nextPageOffset: PageOffset,
        state: State.NewPageProgress,
        page: DataPage<Operation>,
        sideEffectListener: (TransactionStateMachine.SideEffect) -> Unit
    ): State {
        return when (nextPageOffset) {
            is PageOffset.Loadable -> {
                val newData = state.data + page

                // we want to load at least one complete page without user scrolling
                // we also don't wont to stop loading if no relevant items were fetched
                if (newData.size < TransactionStateMachine.PAGE_SIZE || page.isEmpty()) {
                    sideEffectListener(TransactionStateMachine.SideEffect.LoadPage(nextPageOffset, state.usedFilters))

                    State.NewPageProgress(
                        nextPageOffset = nextPageOffset,
                        data = newData,
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                } else {
                    State.Data(
                        nextPageOffset = nextPageOffset,
                        data = newData,
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                }
            }

            PageOffset.FullData -> State.FullData(
                data = state.data + page,
                allAvailableFilters = state.allAvailableFilters,
                usedFilters = state.usedFilters
            )
        }
    }
}
