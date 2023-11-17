package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.model.nativeMinimumBalance
import io.novafoundation.nova.feature_swap_api.domain.model.requireNativeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InsufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.maxAmountToSwap
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapAmountInFeeToken
import io.novafoundation.nova.feature_swap_impl.domain.validation.toBuyAmountToKeepMainEDInFeeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.totalDeductedAmountInFeeToken

class SwapFeeSufficientBalanceValidation : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val swapAmountInFeeToken = value.swapAmountInFeeToken
        val totalDeductedAmount = value.totalDeductedAmountInFeeToken

        if (value.feeAsset.transferableInPlanks < swapAmountInFeeToken + totalDeductedAmount) {
            val chainAssetIn = value.detailedAssetIn.asset.token.configuration
            val feeAsset = value.feeAsset.token.configuration
            val maxAmountToSwap = value.maxAmountToSwap
            val toBuyAmountToKeepEDInFeeAsset = value.toBuyAmountToKeepMainEDInFeeAsset

            return if (toBuyAmountToKeepEDInFeeAsset.isZero) {
                InsufficientBalance.NoNeedsToBuyMainAssetED(chainAssetIn, feeAsset, maxAmountToSwap, value.swapFee.networkFee).validationError()
            } else {
                InsufficientBalance.NeedsToBuyMainAssetED(
                    value.feeAsset.token.configuration,
                    chainAssetIn,
                    value.swapFee.minimumBalanceBuyIn.requireNativeAsset(),
                    toBuyAmountToKeepEDInCommissionAsset = value.swapFee.minimumBalanceBuyIn.nativeMinimumBalance,
                    toSellAmountToKeepEDUsingAssetIn = toBuyAmountToKeepEDInFeeAsset,
                    maxAmountToSwap,
                    value.swapFee.networkFee
                ).validationError()
            }
        }

        return valid()
    }
}
