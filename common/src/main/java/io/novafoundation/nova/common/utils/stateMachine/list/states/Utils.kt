package io.novafoundation.nova.common.utils.stateMachine.list.states

import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateTransition


suspend fun <T> PaginatedListStateTransition<T>.fullDataLoaded(query: String, event: PaginatedListStateMachine.Event.NewPage<T>) {
    emitState(FullDataState(query, event.newPage))
    emitSideEffect(PaginatedListStateMachine.SideEffect.LastPageReached)
}
