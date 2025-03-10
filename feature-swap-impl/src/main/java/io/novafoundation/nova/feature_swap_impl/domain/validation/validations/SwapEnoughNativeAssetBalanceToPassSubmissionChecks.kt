package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_account_api.data.model.amountByExecutingAccount
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure.NotEnoughFunds
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.InsufficientBalanceToStayAboveEDError
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.getExistentialDeposit
import io.novafoundation.nova.runtime.ext.isUtilityAsset

/**
 * Checks that operation can pass submission checks on node side
 * In particular, it checks that there is enough native asset to pay fee and remain above ED
 * This only applies when fee is paid in native asset
 */
class SwapEnoughNativeAssetBalanceToPayFeeConsideringEDValidation(
    private val assetsValidationContext: AssetsValidationContext,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val initialSubmissionFee = value.fee.initialSubmissionFee
        val initialSubmissionFeeChainAsset = initialSubmissionFee.asset

        if (!initialSubmissionFeeChainAsset.isUtilityAsset) return valid()

        val initialSubmissionFeeAsset = assetsValidationContext.getAsset(initialSubmissionFeeChainAsset)
        val existentialDeposit = assetsValidationContext.getExistentialDeposit(initialSubmissionFeeChainAsset)

        val availableBalance = initialSubmissionFeeAsset.balanceCountedTowardsEDInPlanks
        val fee = initialSubmissionFee.amountByExecutingAccount

        return validOrError(availableBalance - fee >= existentialDeposit) {
            val minRequiredBalance = existentialDeposit + fee

            NotEnoughFunds.ToPayFeeAndStayAboveED(
                asset = initialSubmissionFeeChainAsset,
                errorModel = InsufficientBalanceToStayAboveEDError.ErrorModel(
                    minRequiredBalance = initialSubmissionFeeChainAsset.amountFromPlanks(minRequiredBalance),
                    availableBalance = initialSubmissionFeeChainAsset.amountFromPlanks(availableBalance),
                    balanceToAdd = initialSubmissionFeeChainAsset.amountFromPlanks(minRequiredBalance - availableBalance)
                )
            )
        }
    }
}

fun SwapValidationSystemBuilder.sufficientNativeBalanceToPayFeeConsideringED(
    assetsValidationContext: AssetsValidationContext
) {
    validate(
        SwapEnoughNativeAssetBalanceToPayFeeConsideringEDValidation(
            assetsValidationContext = assetsValidationContext
        )
    )
}
