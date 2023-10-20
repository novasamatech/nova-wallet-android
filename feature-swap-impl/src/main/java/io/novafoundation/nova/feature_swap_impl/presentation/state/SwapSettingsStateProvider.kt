package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.feature_swap_impl.domain.slippage.SlippageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

fun SwapSettingsStateProvider.swapSettingsFlow(coroutineScope: CoroutineScope): Flow<SwapSettings> {
    return flowOfAll {
        getSwapSettingsState(coroutineScope).selectedOption
    }
}

class RealSwapSettingsStateProvider(
    private val computationalCache: ComputationalCache,
    private val slippageRepository: SlippageRepository
) : SwapSettingsStateProvider {

    override suspend fun getSwapSettingsState(coroutineScope: CoroutineScope): RealSwapSettingsState {
        return computationalCache.useCache("SwapSettingsState", coroutineScope) {
            RealSwapSettingsState(slippageRepository)
        }
    }
}
