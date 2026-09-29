package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload

class SwapPriceImpactValidation(
    private val priceImpactThresholds: PriceImpactThresholds
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val priceImpact = value.swapQuote.priceImpact

        if (priceImpact > priceImpactThresholds.mediumPriceImpact) {
            return SwapValidationFailure.HighPriceImpact(priceImpact).validationError()
        }

        return valid()
    }
}
