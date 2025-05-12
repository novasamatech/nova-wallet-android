package io.novafoundation.nova.common.utils.stateMachine.list.states

import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.hasNext
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateTransition


class NewPageProgressState<T>(
    query: String,
    private val nextPageOffset: PageOffset.Loadable,
    val data: List<T>
) : PaginatedListState<T>(query) {

    context(PaginatedListStateTransition<T>)
    override suspend fun performTransition(event: PaginatedListStateMachine.Event<T>) {
        when (event) {
            is PaginatedListStateMachine.Event.NewPage -> when {
                shouldIgnoreData(event.usedQuery) -> Unit

                event.newPage.nextOffset.hasNext() -> {
                    val newState = DataState(
                        query = query,
                        nextPageOffset = event.newPage.nextOffset,
                        data = data + event.newPage
                    )

                    emitState(newState)
                }

                else -> fullDataLoaded(query, event)
            }

            is PaginatedListStateMachine.Event.PageError -> handlePageError(event, DataState(query, data, nextPageOffset))

            is PaginatedListStateMachine.Event.Scrolled -> Unit

            is PaginatedListStateMachine.Event.QueryChanged -> handleQueryChanged(event)
        }
    }

    override fun toString(): String {
        return "NewPageProgressState(query=${query}, nextPageOffset=${nextPageOffset}, dataSize=${data.size})"
    }
}

