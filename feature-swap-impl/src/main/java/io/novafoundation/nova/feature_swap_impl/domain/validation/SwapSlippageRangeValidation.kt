package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InvalidSlippage

class SwapSlippageRangeValidation(
    private val assetExchange: AssetExchange
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val slippageConfig = assetExchange.slippageConfig()
        if (value.slippage !in slippageConfig.minAvailableSlippage..slippageConfig.maxAvailableSlippage) {
            return InvalidSlippage.validationError()
        }

        return valid()
    }
}
