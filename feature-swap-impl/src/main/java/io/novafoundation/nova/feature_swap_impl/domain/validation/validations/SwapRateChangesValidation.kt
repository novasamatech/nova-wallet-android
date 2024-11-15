package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.quote
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NewRateExceededSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.estimatedSwapLimit
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class SwapRateChangesValidation(
    private val quoteValidationRetriever: SharedQuoteValidationRetriever,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val newQuote = quoteValidationRetriever.retrieveQuote(value).getOrThrow()
        val swapLimit = value.estimatedSwapLimit()

        return validOrError(swapLimit.isBalanceInSwapLimits(newQuote.quotedPath.quote)) {
            NewRateExceededSlippage(
                assetIn = value.amountIn.chainAsset,
                assetOut = value.amountOut.chainAsset,
                selectedRate = value.swapQuote.swapRate(),
                newRate = newQuote.swapRate()
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
