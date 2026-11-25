package io.novafoundation.nova.feature_swap_impl.data.exchanger

import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteArgs
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAAmountConstraints
import io.novafoundation.nova.feature_swap_impl.domain.exchanger.ExchangerAQuoteResult

class ExchangerAQuoteRepository(
    private val source: ExchangerAQuoteSource,
    private val amountConstraints: ExchangerAAmountConstraints
) {

    suspend fun quote(args: ExchangerAQuoteArgs): ExchangerAQuoteResult {
        val request = mapExchangerAQuoteRequest(args, amountConstraints)
        val response = source.quote(request)
        return mapExchangerAQuote(response, args)
    }
}
