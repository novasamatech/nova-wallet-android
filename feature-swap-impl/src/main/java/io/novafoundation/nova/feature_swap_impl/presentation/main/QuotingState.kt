package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed class QuotingState {

    object Default : QuotingState()

    object Loading : QuotingState()

    object NotAvailable : QuotingState()

    class Loaded(val value: SwapQuote, val quoteArgs: SwapQuoteArgs, val feeAsset: Chain.Asset) : QuotingState()
}
