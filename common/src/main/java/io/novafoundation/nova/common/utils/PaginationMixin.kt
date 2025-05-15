package io.novafoundation.nova.common.utils

import io.novafoundation.nova.common.domain.mapList
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.SideEffect
import io.novafoundation.nova.common.utils.stateMachine.list.states.NewPageProgressState
import io.novafoundation.nova.common.utils.stateMachine.list.toLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class PaginationMixin<T>(
    private val coroutineScope: CoroutineScope
) : CoroutineScope by coroutineScope {

    protected val listStateMachine = PaginatedListStateMachine<T>(coroutineScope)

    val stateFlow = listStateMachine.state
        .shareInBackground()

    private val loadingListState = stateFlow.toLoading()
        .shareInBackground()

    val itemsFlow = loadingListState.map { items ->
        items.mapList { it }
    }

    val isNewPageLoading = stateFlow
        .map { it is NewPageProgressState }
        .shareInBackground()

    val searchInput = MutableStateFlow("")

    fun init() {
        launch {
            for (effect in listStateMachine.sideEffects) {
                when (effect) {
                    is SideEffect.LoadPage -> loadPage(effect)
                    is SideEffect.PresentError -> presentError()
                }
            }
        }

        searchInput.skipFirst()
            .onEach {
                listStateMachine.onEvent(PaginatedListStateMachine.Event.QueryChanged(it))
            }
            .launchIn(this)
    }

    fun onScroll(lastVisiblePosition: Int) {
        listStateMachine.onEvent(PaginatedListStateMachine.Event.Scrolled(lastVisiblePosition))
    }

    abstract suspend fun loadPage(event: SideEffect.LoadPage)

    abstract fun presentError()
}
