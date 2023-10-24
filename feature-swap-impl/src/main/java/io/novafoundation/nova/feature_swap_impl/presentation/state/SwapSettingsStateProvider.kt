package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SwapSettingsStateProvider {

    suspend fun getSwapSettingsState(coroutineScope: CoroutineScope): SwapSettingsState
}

fun SwapSettingsStateProvider.swapSettingsFlow(coroutineScope: CoroutineScope): Flow<SwapSettings> {
    return flowOfAll {
        getSwapSettingsState(coroutineScope).selectedOption
    }
}

class RealSwapSettingsStateProvider(
    private val computationalCache: ComputationalCache,
    private val chainRegistry: ChainRegistry
) : SwapSettingsStateProvider {

    override suspend fun getSwapSettingsState(coroutineScope: CoroutineScope): SwapSettingsState {
        return computationalCache.useCache("SwapSettingsState", coroutineScope) {
            SwapSettingsState(chainRegistry)
        }
    }
}
