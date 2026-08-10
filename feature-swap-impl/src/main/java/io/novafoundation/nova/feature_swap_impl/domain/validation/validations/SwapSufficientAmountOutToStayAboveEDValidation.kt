package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_swap_api.domain.model.amountOutMin
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.getExistentialDeposit

class SwapSufficientAmountOutToStayAboveEDValidation(
    private val assetsValidationContext: AssetsValidationContext,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val lastSegment = value.fee.segments.last()

        // Worst-case output that actually lands (slippage floor), net of the Nova commission charged on
        // this segment (serviceCommission is non-null only when the last segment charges it). Mirrors the
        // on-chain keep-alive so a swap near ED doesn't pass validation only to revert on submission.
        val amountOutMin = lastSegment.operation.estimatedSwapLimit.amountOutMin
        val commission = lastSegment.fee.serviceCommission?.amount.orZero()
        val netAmountOut = (amountOutMin - commission).atLeastZero()

        val assetOut = lastSegment.operation.assetOut
        val existentialDeposit = assetsValidationContext.getExistentialDeposit(assetOut)
        val outAssetBalance = assetsValidationContext.getAsset(assetOut)

        val remainingAmountStaysAboveED = outAssetBalance.balanceCountedTowardsEDInPlanks + netAmountOut >= existentialDeposit

        return validOrError(remainingAmountStaysAboveED) {
            SwapValidationFailure.AmountOutIsTooLowToStayAboveED(
                asset = outAssetBalance.token.configuration,
                amountInPlanks = netAmountOut,
                existentialDeposit = existentialDeposit
            )
        }
    }
}

fun SwapValidationSystemBuilder.sufficientAmountOutToStayAboveEDValidation(assetsValidationContext: AssetsValidationContext) = validate(
    SwapSufficientAmountOutToStayAboveEDValidation(assetsValidationContext)
)
