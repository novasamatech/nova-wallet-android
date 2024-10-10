package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import io.novafoundation.nova.common.data.memory.ComputationalCache
import kotlinx.coroutines.CoroutineScope

interface SwapStateStoreProvider {

    suspend fun getStore(computationScope: CoroutineScope): SwapStateStore
}

class RealSwapStateStoreProvider(
    private val computationalCache: ComputationalCache
): SwapStateStoreProvider {

    companion object {
        private const val CACHE_TAG = "RealSwapQuoteStoreProvider"
    }

    override suspend fun getStore(computationScope: CoroutineScope): SwapStateStore {
        return computationalCache.useCache(CACHE_TAG, computationScope) {
            InMemorySwapStateStore()
        }
    }
}

suspend fun SwapStateStoreProvider.getStateOrThrow(computationScope: CoroutineScope): SwapState {
    return getStore(computationScope).getStateOrThrow()
}
