package io.novafoundation.nova.feature_swap_api.presentation.navigation

import kotlinx.coroutines.CoroutineScope

interface SwapFlowScopeAggregator {

    fun getFlowScope(screenScope: CoroutineScope): CoroutineScope
}
