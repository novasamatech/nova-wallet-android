package io.novafoundation.nova.common.utils.stateMachine.list

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.stateMachine.list.states.DataState
import io.novafoundation.nova.common.utils.stateMachine.list.states.EmptyProgressState
import io.novafoundation.nova.common.utils.stateMachine.list.states.EmptyState
import io.novafoundation.nova.common.utils.stateMachine.list.states.FullDataState
import io.novafoundation.nova.common.utils.stateMachine.list.states.NewPageProgressState
import io.novafoundation.nova.common.utils.stateMachine.list.states.PaginatedListState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> PaginatedListState<T>.toLoadingState(): ExtendedLoadingState<List<T>> {
    return when (this) {
        is DataState -> ExtendedLoadingState.Loaded(data)
        is EmptyProgressState -> ExtendedLoadingState.Loading
        is EmptyState -> ExtendedLoadingState.Loaded(emptyList())
        is FullDataState -> ExtendedLoadingState.Loaded(data)
        is NewPageProgressState -> ExtendedLoadingState.Loaded(data)
    }
}

fun <T> Flow<PaginatedListState<T>>.toLoading(): Flow<ExtendedLoadingState<List<T>>> {
    return map { it.toLoadingState() }
}

val <T> PaginatedListState<T>.dataOrNull: List<T>?
    get() = when (this) {
        is DataState -> data
        is EmptyProgressState -> null
        is EmptyState -> emptyList()
        is FullDataState -> data
        is NewPageProgressState -> data
    }
