package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InvalidSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload

class SwapSlippageRangeValidation(
    private val swapService: SwapService
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val slippageConfig = swapService.slippageConfig(value.detailedAssetIn.chain.id)!!

        if (value.slippage.value !in slippageConfig.minAvailableSlippage.value..slippageConfig.maxAvailableSlippage.value) {
            return InvalidSlippage(slippageConfig.minAvailableSlippage, slippageConfig.maxAvailableSlippage).validationError()
        }

        return valid()
    }
}
