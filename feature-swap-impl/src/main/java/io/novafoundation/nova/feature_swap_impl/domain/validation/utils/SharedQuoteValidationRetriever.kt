package io.novafoundation.nova.feature_swap_impl.domain.validation.utils

import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload

class SharedQuoteValidationRetriever(
    private val swapService: SwapService
) {

    private var result: Result<SwapQuote>? = null

    suspend fun retrieveQuote(value: SwapValidationPayload): Result<SwapQuote> {
        if (result == null) {
            result = swapService.quote(value.swapQuoteArgs)
        }

        return result!!
    }
}
