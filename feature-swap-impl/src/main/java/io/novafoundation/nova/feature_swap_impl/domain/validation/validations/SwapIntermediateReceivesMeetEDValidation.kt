package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.amountOutMin
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext

class SwapIntermediateReceivesMeetEDValidation(private val assetsValidationContext: AssetsValidationContext) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        value.fee.segments
            .dropLast(1) // Last segment destination is verified in a separate validation
            .onEach { swapSegment ->
                val status = checkSegmentDestinationMeetsEd(swapSegment)
                if (status is ValidationStatus.NotValid) return status
            }

        return valid()
    }

    private suspend fun checkSegmentDestinationMeetsEd(segment: SwapFee.SwapSegment): ValidationStatus<SwapValidationFailure>? {
        val amountOutMin = segment.operation.estimatedSwapLimit.amountOutMin
        val assetOut = segment.operation.assetOut

        val existentialDeposit = assetsValidationContext.getExistentialDeposit(assetOut)
        val outAssetBalance = assetsValidationContext.getAsset(assetOut)

        return validOrError(outAssetBalance.balanceCountedTowardsEDInPlanks + amountOutMin >= existentialDeposit) {
            SwapValidationFailure.IntermediateAmountOutIsTooLowToStayAboveED(
                asset = outAssetBalance.token.configuration,
                existentialDeposit = existentialDeposit,
                amount = amountOutMin
            )
        }
    }
}

fun SwapValidationSystemBuilder.intermediateReceivesMeetEDValidation(
    assetsValidationContext: AssetsValidationContext
) = validate(SwapIntermediateReceivesMeetEDValidation(assetsValidationContext))
