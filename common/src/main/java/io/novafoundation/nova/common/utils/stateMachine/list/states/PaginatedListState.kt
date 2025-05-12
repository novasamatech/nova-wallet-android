package io.novafoundation.nova.common.utils.stateMachine.list.states

import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.Event
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.Event.PageError
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.Event.QueryChanged
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.SideEffect
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.SideEffect.LoadPage
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.SideEffect.PresentError
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateTransition

sealed class PaginatedListState<T>(
    val query: String,
) : StateMachine.State<PaginatedListState<T>, SideEffect, Event<T>> {

    protected fun shouldIgnoreData(usedQuery: String) = usedQuery != query

    context(PaginatedListStateTransition<T>)
    protected suspend fun handleQueryChanged(event: QueryChanged) {
        emitState(EmptyProgressState(event.newQuery))
        emitSideEffect(LoadPage(PageOffset.Loadable.FirstPage, event.newQuery))
    }

    context(PaginatedListStateTransition<T>)
    protected suspend fun handlePageError(event: PageError, nextState: PaginatedListState<T>) {
        emitState(nextState)
        emitSideEffect(PresentError(event.error))
    }
}
