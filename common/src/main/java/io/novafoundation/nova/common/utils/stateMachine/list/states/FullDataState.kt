package io.novafoundation.nova.common.utils.stateMachine.list.states

import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateTransition

class FullDataState<T>(query: String, val data: List<T>) : PaginatedListState<T>(query) {

    context(PaginatedListStateTransition<T>)
    override suspend fun performTransition(event: PaginatedListStateMachine.Event<T>) {
        when (event) {
            is PaginatedListStateMachine.Event.NewPage -> Unit
            is PaginatedListStateMachine.Event.PageError -> Unit
            is PaginatedListStateMachine.Event.Scrolled -> Unit
            is PaginatedListStateMachine.Event.QueryChanged -> handleQueryChanged(event)
        }
    }

    override fun toString(): String {
        return "FullDataState(query=$query, dataSize=${data.size})"
    }
}
