package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs

sealed class QuotingState {

    object Default : QuotingState()

    object Loading : QuotingState()

    object NotAvailable : QuotingState()

    data class Loaded(val value: SwapQuote, val quoteArgs: SwapQuoteArgs) : QuotingState()
}
