package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.runtime.state.SelectedOptionSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class LastQuoteStoreSharedStateProvider(
    private val computationalCache: ComputationalCache,
) {

    suspend fun create(coroutineScope: CoroutineScope): LastQuoteStoreSharedState {
        return computationalCache.useCache("LastQuoteStoreSharedState", coroutineScope) {
            RealLastQuoteStoreSharedState()
        }
    }
}

interface LastQuoteStoreSharedState : SelectedOptionSharedState<Pair<SwapQuoteArgs, SwapQuote>?> {

    fun getLastQuote(): Pair<SwapQuoteArgs, SwapQuote>?

    fun setLastQuote(quote: Pair<SwapQuoteArgs, SwapQuote>?)
}

class RealLastQuoteStoreSharedState : LastQuoteStoreSharedState {

    override val selectedOption = MutableStateFlow<Pair<SwapQuoteArgs, SwapQuote>?>(null)

    override fun getLastQuote(): Pair<SwapQuoteArgs, SwapQuote>? {
        return selectedOption.value
    }

    override fun setLastQuote(quote: Pair<SwapQuoteArgs, SwapQuote>?) {
        selectedOption.value = quote
    }
}
