package io.novafoundation.nova.feature_swap_impl.presentation.common.navigation

import io.novafoundation.nova.common.utils.invokeOnCompletion
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.coroutines.EmptyCoroutineContext

class RealSwapFlowScopeAggregator : SwapFlowScopeAggregator {

    private var aggregatedScope: CoroutineScope? = null
    private val scopes = mutableSetOf<CoroutineScope>()

    private val lock = Any()

    override fun getFlowScope(screenScope: CoroutineScope): CoroutineScope {
        synchronized(lock) {
            if (aggregatedScope == null) {
                aggregatedScope = CoroutineScope(EmptyCoroutineContext)
            }

            scopes.add(screenScope)
        }

        screenScope.invokeOnCompletion {
            synchronized(lock) {
                scopes -= screenScope

                if (scopes.isEmpty()) {
                    aggregatedScope!!.cancel()
                    aggregatedScope = null
                }
            }
        }

        return aggregatedScope!!
    }
}
