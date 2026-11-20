package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapDustCheckFee
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.TooSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.getExistentialDeposit
import io.novasama.substrate_sdk_android.hash.isPositive

class SwapDoNotLooseAssetInDustValidation(
    private val assetsValidationContext: AssetsValidationContext,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val chainAssetIn = value.amountIn.chainAsset

        val balanceCountedTowardsEd = assetsValidationContext.getAsset(chainAssetIn).balanceCountedTowardsEDInPlanks
        val swapAmount = value.amountIn.amount
        val assetInExistentialDeposit = assetsValidationContext.getExistentialDeposit(chainAssetIn)

        // Max already reserves the fee with its margin. Only the balance beyond that reserve can be added to the swap.
        val totalFees = when (val fee = value.dustCheckFee) {
            SwapDustCheckFee.QuoteFee -> value.fee.totalFeeAmount(chainAssetIn)
            is SwapDustCheckFee.MaxFeeWithMargin -> fee.amount
        }
        val remainingBalance = balanceCountedTowardsEd - swapAmount - totalFees

        if (remainingBalance.isPositive() && remainingBalance < assetInExistentialDeposit) {
            return TooSmallRemainingBalance(chainAssetIn, remainingBalance, assetInExistentialDeposit).validationError()
        }

        return valid()
    }
}
