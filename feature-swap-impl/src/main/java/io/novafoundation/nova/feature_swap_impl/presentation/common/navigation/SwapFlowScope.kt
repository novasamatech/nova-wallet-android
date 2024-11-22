package io.novafoundation.nova.feature_swap_impl.presentation.common.navigation

import android.util.Log
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

            Log.d("Swaps", "Registering new swap screen scope, total count: ${scopes.size}")
        }

        screenScope.invokeOnCompletion {
            synchronized(lock) {
                scopes -= screenScope

                if (scopes.isEmpty()) {
                    Log.d("Swaps", "Last swap screen scope was cancelled, cancelling flow scope")

                    aggregatedScope!!.cancel()
                    aggregatedScope = null
                } else {
                    Log.d("Swaps", "Swap screen scope was cancelled, remaining count: ${scopes.size}")
                }
            }
        }

        return aggregatedScope!!
    }
}
