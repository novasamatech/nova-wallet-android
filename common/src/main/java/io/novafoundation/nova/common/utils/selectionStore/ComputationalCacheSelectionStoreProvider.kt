package io.novafoundation.nova.common.utils.selectionStore

import io.novafoundation.nova.common.data.memory.ComputationalCache
import kotlinx.coroutines.CoroutineScope

abstract class ComputationalCacheSelectionStoreProvider<T : SelectionStore<*>>(
    private val computationalCache: ComputationalCache,
    private val key: String
) : SelectionStoreProvider<T> {

    override suspend fun getSelectionStore(scope: CoroutineScope): T {
        return computationalCache.useCache(key, scope) {
            initSelectionStore()
        }
    }

    protected abstract fun initSelectionStore(): T
}
