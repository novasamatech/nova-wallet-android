package io.novafoundation.nova.common.utils.stateMachine.list.states

import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.hasNext
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateTransition

class EmptyProgressState<T>(query: String) : PaginatedListState<T>(query) {

    context(PaginatedListStateTransition<T>)
    override suspend fun performTransition(event: PaginatedListStateMachine.Event<T>) {
        when (event) {
            is PaginatedListStateMachine.Event.NewPage -> when {
                shouldIgnoreData(event.usedQuery) -> Unit
                event.newPage.isEmpty() -> emitState(EmptyState(query))
                event.newPage.nextOffset.hasNext() -> emitState(DataState(query, event.newPage, event.newPage.nextOffset))

                else -> emitState(FullDataState(query, event.newPage))
            }

            is PaginatedListStateMachine.Event.PageError -> handlePageError(event, EmptyState(query))

            is PaginatedListStateMachine.Event.Scrolled -> Unit

            is PaginatedListStateMachine.Event.QueryChanged -> handleQueryChanged(event)
        }
    }

    context(PaginatedListStateTransition<T>)
    override suspend fun bootstrap() {
        emitSideEffect(
            PaginatedListStateMachine.SideEffect.LoadPage(
                PageOffset.Loadable.FirstPage,
                ""
            )
        )
    }

    override fun toString(): String {
        return "EmptyProgressState(query=$query)"
    }
}
