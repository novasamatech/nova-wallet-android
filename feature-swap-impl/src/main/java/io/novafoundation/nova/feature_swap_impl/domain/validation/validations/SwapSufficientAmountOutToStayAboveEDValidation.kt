package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
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

        // Worst-case output that actually lands (slippage floor), already net of the Nova commission carried
        // down the route. Mirrors the on-chain keep-alive so a swap near ED doesn't pass validation only to
        // revert on submission.
        val netAmountOut = lastSegment.netFlow.amountOutMin

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
