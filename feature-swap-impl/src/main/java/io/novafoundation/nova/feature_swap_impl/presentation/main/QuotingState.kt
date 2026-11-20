package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.routeId
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload

sealed class QuotingState {

    object Default : QuotingState()

    object Loading : QuotingState()

    data class Error(val error: Throwable) : QuotingState()

    data class Loaded(
        val quote: SwapQuote,
        val quoteArgs: SwapQuoteArgs,
        val origin: QuoteRefreshOrigin,
        val settings: SwapSettings,
    ) : QuotingState()
}

enum class QuoteRefreshOrigin {
    USER_INPUT,
    SUBSCRIPTION,
}

internal class SwapFeeSnapshot(val quote: QuotingState.Loaded, val fee: SwapFee) {

    fun matchesInputs(settings: SwapSettings, metaAccountId: Long?): Boolean {
        return quote.settings == settings && quote.quoteArgs.metaAccountId == metaAccountId
    }

    fun matchesQuote(other: QuotingState.Loaded): Boolean {
        return matchesInputs(other.settings, other.quoteArgs.metaAccountId) &&
            fee.route == other.quote.quotedPath.path.map { it.edge.routeId() }
    }

    fun toValidationPayload(): SwapValidationPayload {
        return SwapValidationPayload(fee, quote.quote, quote.settings.slippage, quote.quoteArgs.metaAccountId)
    }
}

inline fun <T> QuotingState.toLoadingState(onLoaded: (SwapQuote) -> T?): ExtendedLoadingState<T?> {
    return when (this) {
        QuotingState.Default -> ExtendedLoadingState.Loaded(null)
        is QuotingState.Error -> ExtendedLoadingState.Error(error)
        is QuotingState.Loaded -> ExtendedLoadingState.Loaded(onLoaded(quote))
        QuotingState.Loading -> ExtendedLoadingState.Loading
    }
}
