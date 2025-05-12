package io.novafoundation.nova.common.utils.stateMachine.list.states

import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateTransition

suspend fun <T> PaginatedListStateTransition<T>.fullDataLoaded(query: String, page: List<T>) {
    emitState(FullDataState(query, page))
    emitSideEffect(PaginatedListStateMachine.SideEffect.LastPageReached)
}
