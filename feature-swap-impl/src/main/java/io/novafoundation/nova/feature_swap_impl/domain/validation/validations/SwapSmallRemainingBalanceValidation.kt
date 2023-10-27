package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.model.nativeMinimumBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.TooSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.toBuyEDInFeeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.totalDeductedAmountInFeeToken
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount

class SwapSmallRemainingBalanceValidation : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val feeChainAsset = value.feeAsset.token.configuration
        val chainAssetIn = value.detailedAssetIn.asset.token.configuration
        val chainAssetOut = value.detailedAssetIn.asset.token.configuration

        val assetInTotal = value.detailedAssetIn.asset.totalInPlanks
        val toBuyExistentialDeposit = value.toBuyEDInFeeAsset
        val swapAmount = chainAssetIn.planksFromAmount(value.detailedAssetIn.amount)
        val assetInEd = value.swapFee.minimumBalanceBuyIn.nativeMinimumBalance
        val totalDeductedAmount = value.totalDeductedAmountInFeeToken
        val remainingBalance = assetInTotal - swapAmount - totalDeductedAmount

        if (remainingBalance < assetInEd) {
            return if (toBuyExistentialDeposit.isZero) {
                TooSmallRemainingBalance.NoNeedsToBuyMainAssetED(chainAssetIn, remainingBalance, value.swapFee.networkFee).validationError()
            } else {
                TooSmallRemainingBalance.NeedsToBuyMainAssetED(
                    feeChainAsset,
                    chainAssetIn,
                    chainAssetOut,
                    toBuyExistentialDeposit,
                    remainingBalance,
                    value.swapFee.networkFee
                ).validationError()
            }
        }

        return valid()
    }
}
