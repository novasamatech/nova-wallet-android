package io.novafoundation.nova.feature_swap_impl.presentation.main

import io.novafoundation.nova.feature_swap_impl.presentation.main.QuotingState.Loaded.PreviewOnly
import io.novafoundation.nova.feature_swap_impl.presentation.main.QuotingState.Loaded.ReadyForReview
import io.novafoundation.nova.feature_swap_impl.domain.swap.QuoteProviderException
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.ExternalSwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuotePreview
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.routeId
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload

sealed class QuotingState {

    object Default : QuotingState()

    object Loading : QuotingState()

    data class Error(val error: Throwable) : QuotingState()

    class PayLimit(val limit: QuoteProviderException.DepositLimit) : QuotingState()

    sealed class Loaded : QuotingState() {

        abstract val quote: SwapQuotePreview

        // TODO(NW-2502): Remove PreviewOnly once external quotes can proceed to Review.
        class PreviewOnly(override val quote: SwapQuotePreview) : Loaded()

        data class ReadyForReview(
            override val quote: SwapQuote,
            val quoteArgs: SwapQuoteArgs,
            val origin: QuoteRefreshOrigin,
            val settings: SwapSettings,
        ) : Loaded()
    }
}

internal fun QuotingState.readyForReviewOrNull(): ReadyForReview? = when (this) {
    is ReadyForReview -> this
    is PreviewOnly, is QuotingState.PayLimit, is QuotingState.Error, QuotingState.Default, QuotingState.Loading -> null
}

internal fun SwapQuotePreview.toQuotingState(args: SwapQuoteArgs, origin: QuoteRefreshOrigin, settings: SwapSettings): QuotingState {
    return when (this) {
        is SwapQuote -> ReadyForReview(this, args, origin, settings)
        is ExternalSwapQuote -> PreviewOnly(this)
    }
}

enum class QuoteRefreshOrigin {
    USER_INPUT,
    SUBSCRIPTION,
}

internal class SwapFeeSnapshot(val quote: ReadyForReview, val fee: SwapFee) {

    fun matchesInputs(settings: SwapSettings, metaAccountId: Long?): Boolean {
        return quote.settings == settings && quote.quoteArgs.metaAccountId == metaAccountId
    }

    fun matchesQuote(other: ReadyForReview): Boolean {
        return matchesInputs(other.settings, other.quoteArgs.metaAccountId) &&
            fee.route == other.quote.quotedPath.path.map { it.edge.routeId() }
    }

    fun toValidationPayload(): SwapValidationPayload {
        return SwapValidationPayload(fee, quote.quote, quote.settings.slippage, quote.quoteArgs.metaAccountId)
    }
}

inline fun <T> QuotingState.toLoadingState(onLoaded: (SwapQuote) -> T?): ExtendedLoadingState<T?> {
    return when (this) {
        QuotingState.Default, is PreviewOnly, is QuotingState.PayLimit -> ExtendedLoadingState.Loaded(null)
        is QuotingState.Error -> ExtendedLoadingState.Error(error)
        is ReadyForReview -> ExtendedLoadingState.Loaded(onLoaded(quote))
        QuotingState.Loading -> ExtendedLoadingState.Loading
    }
}
