package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InsufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.maxAmountToSwap
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapAmountInFeeToken
import io.novafoundation.nova.feature_swap_impl.domain.validation.totalDeductedAmountInFeeToken

class SwapFeeSufficientBalanceValidation : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val swapAmountInFeeToken = value.swapAmountInFeeToken
        val totalDeductedAmount = value.totalDeductedAmountInFeeToken

        if (value.feeAsset.transferableInPlanks < swapAmountInFeeToken + totalDeductedAmount) {
            val chainAssetIn = value.detailedAssetIn.asset.token.configuration
            val feeAsset = value.feeAsset.token.configuration
            val maxAmountToSwap = value.maxAmountToSwap

            return InsufficientBalance.CannotPayFee(chainAssetIn, feeAsset, maxAmountToSwap, value.fee).validationError()
        }

        return valid()
    }
}
