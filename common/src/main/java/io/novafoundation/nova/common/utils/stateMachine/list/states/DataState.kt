package io.novafoundation.nova.common.utils.stateMachine.list.states

import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateTransition

class DataState<T>(
    query: String,
    val data: List<T>,
    private val nextPageOffset: PageOffset.Loadable,
) : PaginatedListState<T>(query) {

    private val scrollOffset = 10

    context(PaginatedListStateTransition<T>)
    override suspend fun performTransition(event: PaginatedListStateMachine.Event<T>) {
        when (event) {
            is PaginatedListStateMachine.Event.NewPage -> Unit

            is PaginatedListStateMachine.Event.PageError -> Unit

            is PaginatedListStateMachine.Event.Scrolled -> if (shouldStartNextPageFetching(event.currentItemIndex, data.size)) {
                emitState(NewPageProgressState(query, nextPageOffset, data))
                emitSideEffect(PaginatedListStateMachine.SideEffect.LoadPage(nextPageOffset, query))
            }

            is PaginatedListStateMachine.Event.QueryChanged -> handleQueryChanged(event)
        }
    }

    override fun toString(): String {
        return "DataState(query=$query, nextPageOffset=$nextPageOffset, dataSize=${data.size})"
    }

    private fun shouldStartNextPageFetching(currentIndex: Int, listSize: Int): Boolean {
        return currentIndex >= listSize - scrollOffset
    }
}
