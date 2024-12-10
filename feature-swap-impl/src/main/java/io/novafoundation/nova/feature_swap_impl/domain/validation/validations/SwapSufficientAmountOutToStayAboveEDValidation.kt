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
        val (chainAssetOut, amountOut) = value.amountOut

        val assetOut = assetsValidationContext.getAsset(chainAssetOut)
        val existentialDeposit = assetsValidationContext.getExistentialDeposit(assetOut.token.configuration)

        val remainingAmountStaysAboveED = assetOut.balanceCountedTowardsEDInPlanks + amountOut >= existentialDeposit

        return validOrError(remainingAmountStaysAboveED) {
            SwapValidationFailure.AmountOutIsTooLowToStayAboveED(
                asset = assetOut.token.configuration,
                amountInPlanks = amountOut,
                existentialDeposit = existentialDeposit
            )
        }
    }
}

fun SwapValidationSystemBuilder.sufficientAmountOutToStayAboveEDValidation(assetsValidationContext: AssetsValidationContext) = validate(
    SwapSufficientAmountOutToStayAboveEDValidation(assetsValidationContext)
)
