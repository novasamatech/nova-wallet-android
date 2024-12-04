package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_swap_api.domain.model.allFeeAssets
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.InsufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.ext.fullId

/**
 * Validation that checks whether the user can pay all the fees in assets other then assetIn
 * Asset in fees is checked in a separate validation that also takes swap amount into account
 */
class SwapCanPayExtraFeesValidation(
    private val assetsValidationContext: AssetsValidationContext
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val extraFeeAssets = value.fee.firstSegmentFee.allFeeAssets()
            // asset in fee is checked in a separate validation
            .filter { it.fullId != value.amountIn.chainAsset.fullId }

        extraFeeAssets.onEach { feeChainAsset ->
            val feeAsset = assetsValidationContext.getAsset(feeChainAsset)
            val totalFee = value.fee.totalFeeAmount(feeChainAsset)

            val availableBalance = feeAsset.transferableInPlanks

            if (availableBalance < totalFee) {
                return InsufficientBalance.CannotPayFee(feeChainAsset, availableBalance, totalFee)
                    .validationError()
            }
        }

        return valid()
    }
}
