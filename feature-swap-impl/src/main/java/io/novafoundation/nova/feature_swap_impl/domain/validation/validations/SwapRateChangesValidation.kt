package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.toPerbill
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.quotedBalance
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NewRateExceededSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class SwapRateChangesValidation(
    private val getNewRate: suspend (SwapValidationPayload) -> SwapQuote,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val slippage = value.slippage.toPerbill()
        val newQuote = getNewRate(value)
        val rateDifference = value.swapExecuteArgs.swapLimit.calculateRateDifference(newQuote.quotedBalance)

        if (rateDifference > slippage) {
            return NewRateExceededSlippage(
                value.detailedAssetIn.asset.token.configuration,
                value.detailedAssetOut.asset.token.configuration,
                value.swapQuote.swapRate(),
                newQuote.swapRate()
            ).validationError()
        }

        return valid()
    }
}

private fun SwapLimit.calculateRateDifference(quotedBalance: Balance): Perbill {
    val rateDifference = when (this) {
        is SwapLimit.SpecifiedIn -> (amountOutMin.toBigDecimal() - quotedBalance.toBigDecimal()) / quotedBalance.toBigDecimal()
        is SwapLimit.SpecifiedOut -> (quotedBalance.toBigDecimal() - amountInMax.toBigDecimal()) / quotedBalance.toBigDecimal()
    }

    return rateDifference.asPerbill()
}
