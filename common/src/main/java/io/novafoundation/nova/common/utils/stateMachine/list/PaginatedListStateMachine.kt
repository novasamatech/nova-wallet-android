package io.novafoundation.nova.common.utils.stateMachine.list

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.states.EmptyProgressState
import io.novafoundation.nova.common.utils.stateMachine.list.states.PaginatedListState
import kotlinx.coroutines.CoroutineScope

object PaginatedListStateMachine {

    sealed class Event<out T> {

        class Scrolled(val currentItemIndex: Int) : Event<Nothing>()

        data class NewPage<T>(val newPage: DataPage<T>, val usedQuery: String = "") : Event<T>()

        data class PageError(val error: Throwable) : Event<Nothing>()

        data class QueryChanged(val newQuery: String) : Event<Nothing>()
    }

    sealed class SideEffect {

        data class LoadPage(
            val nextPageOffset: PageOffset.Loadable,
            val query: String,
        ) : SideEffect()

        data class PresentError(val error: Throwable) : SideEffect()
    }
}

fun <T> PaginatedListStateMachine(
    coroutineScope: CoroutineScope,
    initialQuery: String = "",
): StateMachine<PaginatedListState<T>, PaginatedListStateMachine.SideEffect, PaginatedListStateMachine.Event<T>> {
    return StateMachine(initialState = EmptyProgressState(query = initialQuery), coroutineScope)
}

internal typealias PaginatedListStateTransition<T> = StateMachine.Transition<PaginatedListState<T>, PaginatedListStateMachine.SideEffect>
