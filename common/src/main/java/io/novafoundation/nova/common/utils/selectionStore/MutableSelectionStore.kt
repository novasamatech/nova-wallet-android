package io.novafoundation.nova.common.utils.selectionStore

import kotlinx.coroutines.flow.MutableStateFlow

abstract class MutableSelectionStore<T> : SelectionStore<T> {

    override val currentSelectionFlow = MutableStateFlow<T?>(null)

    override fun getCurrentSelection(): T? {
        return currentSelectionFlow.value
    }

    fun updateSelection(selection: T) {
        currentSelectionFlow.value = selection
    }
}
