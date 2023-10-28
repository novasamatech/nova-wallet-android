package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.toPerbill
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NewRateExceededSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import java.math.BigDecimal

class SwapRateChangesValidation(
    private val getCurrentRate: suspend (SwapValidationPayload) -> BigDecimal,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val slippage = value.slippage.toPerbill()
        val selectedRate = value.detailedAssetOut.amount / value.detailedAssetIn.amount
        val newRate = getCurrentRate(value)
        val rateDifference = (selectedRate - newRate).abs() / selectedRate
        if (rateDifference > slippage.value.toBigDecimal()) {
            return NewRateExceededSlippage(
                value.detailedAssetIn.asset.token.configuration,
                value.detailedAssetOut.asset.token.configuration,
                selectedRate,
                newRate
            ).validationError()
        }

        return valid()
    }
}
