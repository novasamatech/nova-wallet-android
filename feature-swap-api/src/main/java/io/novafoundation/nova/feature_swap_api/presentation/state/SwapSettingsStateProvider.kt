package io.novafoundation.nova.feature_swap_api.presentation.state

import kotlinx.coroutines.CoroutineScope

interface SwapSettingsStateProvider {

    suspend fun getSwapSettingsState(coroutineScope: CoroutineScope): SwapSettingsState
}
