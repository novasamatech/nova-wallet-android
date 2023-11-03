package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.toPerbill
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
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
        val newQuote = getNewRate(value)
        val swapLimit = value.swapExecuteArgs.swapLimit

        return validOrError(swapLimit.isBalanceInSwapLimits(newQuote.quotedBalance)) {
            NewRateExceededSlippage(
                value.detailedAssetIn.asset.token.configuration,
                value.detailedAssetOut.asset.token.configuration,
                value.swapQuote.swapRate(),
                newQuote.swapRate()
            )
        }
    }
}

private fun SwapLimit.isBalanceInSwapLimits(quotedBalance: Balance): Boolean {
    return when (this) {
        is SwapLimit.SpecifiedIn -> quotedBalance >= amountOutMin
        is SwapLimit.SpecifiedOut -> quotedBalance <= amountInMax
    }
}
