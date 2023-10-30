package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NotEnoughLiquidity
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload

class SwapEnoughLiquidityValidation(
    private val quoteRetriever: suspend (SwapValidationPayload) -> Result<SwapQuote>
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val quoteResult = quoteRetriever(value)

        return validOrError(quoteResult.isSuccess) {
            NotEnoughLiquidity
        }
    }
}
