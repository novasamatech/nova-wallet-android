package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs

sealed class QuotingState {

    object Default : QuotingState()

    object Loading : QuotingState()

    data class Error(val error: Throwable) : QuotingState()

    data class Loaded(val quote: SwapQuote, val quoteArgs: SwapQuoteArgs) : QuotingState()
}

inline fun <T> QuotingState.toLoadingState(onLoaded: (SwapQuote) -> T?): ExtendedLoadingState<T?> {
    return when (this) {
        QuotingState.Default -> ExtendedLoadingState.Loaded(null)
        is QuotingState.Error -> ExtendedLoadingState.Error(error)
        is QuotingState.Loaded -> ExtendedLoadingState.Loaded(onLoaded(quote))
        QuotingState.Loading -> ExtendedLoadingState.Loading
    }
}
