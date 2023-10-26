package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.toPerbill
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InvalidSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NewRateExceededSlippage
import java.math.BigDecimal

class SwapEnoughFeeValidation(
    private val currentRate: suspend (SwapValidationPayload) -> BigDecimal,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val slippage = value.slippage.toPerbill()
        val currentRate = currentRate(value)
        val selectedRate = value.outDetails.amount / value.inDetails.amount
        val rateDifference = (selectedRate - currentRate).abs() / selectedRate
        if (rateDifference > slippage.value.toBigDecimal()) {
            return NewRateExceededSlippage.validationError()
        }

        return valid()
    }
}
