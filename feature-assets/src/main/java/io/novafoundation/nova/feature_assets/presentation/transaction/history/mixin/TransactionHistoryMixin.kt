package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface TransactionHistoryUi {

    class State(
        val filtersButtonVisible: Boolean,
        val listState: ListState,
    ) {

        sealed class ListState {

            object Empty : ListState()

            object EmptyProgress : ListState()

            class Data(val items: List<Any>) : ListState()
        }
    }

    val state: Flow<State>

    fun transactionClicked(transactionId: String)
}

interface TransactionHistoryMixin : TransactionHistoryUi, CoroutineScope {

    suspend fun syncFirstOperationsPage(): Result<*>

    fun scrolled(currentIndex: Int)
}
