package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InsufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.maxAmountToSwap
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapAmountInFeeToken
import io.novafoundation.nova.feature_swap_impl.domain.validation.toBuyAmountToKeepEDInFeeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.totalDeductedAmountInFeeToken
import java.math.BigInteger

class SwapFeeSufficientBalanceValidation : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val swapAmountInFeeToken = value.swapAmountInFeeToken
        val totalDeductedAmount = value.totalDeductedAmountInFeeToken

        if (value.feeAsset.transferableInPlanks < swapAmountInFeeToken + totalDeductedAmount) {
            val chainAssetIn = value.detailedAssetIn.asset.token.configuration
            val chainAssetOut = value.detailedAssetOut.asset.token.configuration
            val feeAsset = value.feeAsset.token.configuration
            val maxAmountToSwap = value.maxAmountToSwap
            val toBuyAmountToKeepEDInFeeAsset = value.toBuyAmountToKeepEDInFeeAsset

            return if (toBuyAmountToKeepEDInFeeAsset.isZero) {
                InsufficientBalance.NoNeedsToBuyMainAssetED(chainAssetIn, feeAsset, value.maxAmountToSwap, value.swapFee.networkFee).validationError()
            } else {
                InsufficientBalance.NeedsToBuyMainAssetED(
                    value.feeAsset.token.configuration,
                    chainAssetIn,
                    toBuyAmountToKeepEDInCommissionAsset = toBuyAmountToKeepEDInFeeAsset,
                    toSellAmountToKeepEDUsingAssetIn = BigInteger.ZERO, // TODO how to convert toBuyAmountToKeepEDInFeeAsset to this value?
                    maxAmountToSwap,
                    value.swapFee.networkFee
                ).validationError()
            }
        }

        return valid()
    }
}
