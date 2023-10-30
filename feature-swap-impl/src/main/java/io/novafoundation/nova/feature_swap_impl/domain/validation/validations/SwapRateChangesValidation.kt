package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.toPerbill
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NewRateExceededSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import java.math.BigDecimal

class SwapRateChangesValidation(
    private val getNewRate: suspend (SwapValidationPayload) -> BigDecimal,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val slippage = value.slippage.toPerbill()
        val oldRate = value.swapQuote.swapRate()
        val newRate = getNewRate(value)
        val deltaRate = oldRate - newRate // negative if rate increased
        val rateDifference = (deltaRate / oldRate).asPerbill()

        // We don't check the case when rate becomes beneficial for the user
        if (rateDifference > slippage) {
            return NewRateExceededSlippage(
                value.detailedAssetIn.asset.token.configuration,
                value.detailedAssetOut.asset.token.configuration,
                oldRate,
                newRate
            ).validationError()
        }

        return valid()
    }
}
