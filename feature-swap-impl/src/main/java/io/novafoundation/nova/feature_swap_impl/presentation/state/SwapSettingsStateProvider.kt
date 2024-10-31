package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_swap_api.presentation.state.DEFAULT_SLIPPAGE
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

fun SwapSettingsStateProvider.swapSettingsFlow(coroutineScope: CoroutineScope): Flow<SwapSettings> {
    return flowOfAll {
        getSwapSettingsState(coroutineScope).selectedOption
    }
}

class RealSwapSettingsStateProvider(
    private val computationalCache: ComputationalCache,
) : SwapSettingsStateProvider {

    override suspend fun getSwapSettingsState(coroutineScope: CoroutineScope): RealSwapSettingsState {
        return computationalCache.useCache("SwapSettingsState", coroutineScope) {
            RealSwapSettingsState(SwapSettings(slippage = DEFAULT_SLIPPAGE))
        }
    }
}
