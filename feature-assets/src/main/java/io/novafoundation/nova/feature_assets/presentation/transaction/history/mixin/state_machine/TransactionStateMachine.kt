package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation

object TransactionStateMachine {

    const val PAGE_SIZE = 100

    interface StateProvider<A : Action> {
        fun getState(action: A, state: State, sideEffectListener: (SideEffect) -> Unit): State

        fun canUseCache(
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>
        ) = TransactionStateMachine.canUseCache(allAvailableFilters, usedFilters)
    }

    sealed class State(
        val allAvailableFilters: Set<TransactionFilter>,
        val usedFilters: Set<TransactionFilter>
    ) {

        interface WithData {
            val data: List<Operation>
        }

        class Empty(
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>
        ) : State(allAvailableFilters, usedFilters)

        class EmptyProgress(
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>
        ) : State(allAvailableFilters, usedFilters)

        class Data(
            val nextPageOffset: PageOffset.Loadable,
            override val data: List<Operation>,
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>,
        ) : State(allAvailableFilters, usedFilters), WithData

        class NewPageProgress(
            val nextPageOffset: PageOffset.Loadable,
            override val data: List<Operation>,
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>,
        ) : State(allAvailableFilters, usedFilters), WithData

        class FullData(
            override val data: List<Operation>,
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>,
        ) : State(allAvailableFilters, usedFilters), WithData
    }

    sealed class Action {

        class Scrolled(val currentItemIndex: Int) : Action()

        data class CachePageArrived(
            val newPage: DataPage<Operation>,
            val accountChanged: Boolean
        ) : Action()

        data class NewPage(val newPage: DataPage<Operation>, val loadedWith: Set<TransactionFilter>) : Action()

        data class PageError(val error: Throwable) : Action()

        class FiltersChanged(val newUsedFilters: Set<TransactionFilter>) : Action()
    }

    sealed class SideEffect {

        data class LoadPage(
            val nextPageOffset: PageOffset.Loadable,
            val filters: Set<TransactionFilter>,
            val pageSize: Int = PAGE_SIZE,
        ) : SideEffect()

        data class ErrorEvent(val error: Throwable) : SideEffect()

        object TriggerCache : SideEffect()
    }

    fun transition(
        action: Action,
        state: State,
        sideEffectListener: (SideEffect) -> Unit,
    ): State = when (action) {
        is Action.CachePageArrived -> OnCacheArrivedStateProvider.getState(action, state, sideEffectListener)

        is Action.Scrolled -> OnScrolledStateProvider.getState(action, state, sideEffectListener)

        is Action.NewPage -> OnNewPageStateProvider.getState(action, state, sideEffectListener)

        is Action.PageError -> OnPageErrorStateProvider.getState(action, state, sideEffectListener)

        is Action.FiltersChanged -> OnFiltersChangedStateProvider.getState(action, state, sideEffectListener)
    }

    private fun canUseCache(
        allAvailableFilters: Set<TransactionFilter>,
        usedFilters: Set<TransactionFilter>
    ) = allAvailableFilters == usedFilters
}
