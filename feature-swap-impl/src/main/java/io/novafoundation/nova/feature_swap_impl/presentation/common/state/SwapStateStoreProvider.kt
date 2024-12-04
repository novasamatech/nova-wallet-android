package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOfAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SwapStateStoreProvider {

    suspend fun getStore(computationScope: CoroutineScope): SwapStateStore
}

class RealSwapStateStoreProvider(
    private val computationalCache: ComputationalCache
) : SwapStateStoreProvider {

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

fun SwapStateStoreProvider.stateFlow(computationScope: CoroutineScope): Flow<SwapState> {
    return flowOfAll { getStore(computationScope).stateFlow() }
}

context(BaseViewModel)
suspend fun SwapStateStoreProvider.setState(state: SwapState) {
    getStore(viewModelScope).setState(state)
}
