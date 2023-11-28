package io.novafoundation.nova.common.utils.selectionStore

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SelectionStore<T> {

    val currentSelectionFlow: Flow<T?>

    fun getCurrentSelection(): T?
}

interface SelectionStoreProvider<T : SelectionStore<*>> {

    suspend fun getSelectionStore(scope: CoroutineScope): T
}
