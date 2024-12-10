package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novasama.substrate_sdk_android.hash.isPositive

/**
 * Checks that spending assetIn to swap for assetOut wont dust account and result in assetOut being lost
 */
class SwapKeepsNecessaryAmountOfAssetIn(
    private val assetsValidationContext: AssetsValidationContext,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val chainAssetIn = value.amountIn.chainAsset
        val chainAssetOut = value.amountOut.chainAsset

        val amount = value.amountIn.amount

        val minimumCountedTowardsEd = value.fee.additionalMaxAmountDeduction.fromCountedTowardsEd

        if (minimumCountedTowardsEd.isPositive()) {
            val assetIn = assetsValidationContext.getAsset(chainAssetIn)
            val fee = value.fee.totalFeeAmount(chainAssetIn)

            val assetInStaysAboveEd = assetIn.balanceCountedTowardsEDInPlanks - amount - fee >= minimumCountedTowardsEd

            return validOrError(assetInStaysAboveEd) {
                SwapValidationFailure.InsufficientBalance.BalanceNotConsiderInsufficientReceiveAsset(
                    assetIn = chainAssetIn,
                    assetOut = chainAssetOut,
                    existentialDeposit = minimumCountedTowardsEd
                )
            }
        }

        return valid()
    }
}

fun SwapValidationSystemBuilder.sufficientBalanceConsideringNonSufficientAssetsValidation(assetsValidationContext: AssetsValidationContext) = validate(
    SwapKeepsNecessaryAmountOfAssetIn(assetsValidationContext)
)
