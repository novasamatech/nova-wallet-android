package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationFailure

fun BittensorClaimValidationFailure.message(resources: ResourceManager): String = resources.getString(
    when (this) {
        BittensorClaimValidationFailure.CLAIM_PENDING -> R.string.bittensor_claim_pending
        BittensorClaimValidationFailure.COLDKEY_SWAP_PENDING -> R.string.bittensor_validation_coldkey_swap
        BittensorClaimValidationFailure.SAFE_MODE_ACTIVE -> R.string.bittensor_validation_safe_mode
        BittensorClaimValidationFailure.NOT_CLAIMABLE -> R.string.bittensor_claim_none
        BittensorClaimValidationFailure.REWARDS_DECREASED -> R.string.bittensor_claim_rewards_decreased
        BittensorClaimValidationFailure.FORFEITURE_INCREASED -> R.string.bittensor_claim_forfeiture_increased
        BittensorClaimValidationFailure.INSUFFICIENT_FEE_BALANCE -> R.string.bittensor_validation_fee_reserve
        BittensorClaimValidationFailure.UNECONOMIC_CLAIM -> R.string.bittensor_claim_uneconomic_message
    }
)

fun bittensorClaimValidationFailureUi(
    reason: BittensorClaimValidationFailure,
    resources: ResourceManager,
    reviewUpdated: () -> Unit,
    acceptUneconomic: () -> Unit
): TransformedFailure {
    val action = when (reason) {
        BittensorClaimValidationFailure.REWARDS_DECREASED, BittensorClaimValidationFailure.FORFEITURE_INCREASED ->
            CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.bittensor_claim_review_updated), reviewUpdated)
        BittensorClaimValidationFailure.UNECONOMIC_CLAIM ->
            CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_continue), acceptUneconomic)
        else -> return TransformedFailure.Default(resources.getString(R.string.common_error_general_title) to reason.message(resources))
    }
    val title = if (reason == BittensorClaimValidationFailure.UNECONOMIC_CLAIM) {
        R.string.bittensor_claim_uneconomic_title
    } else {
        R.string.common_error_general_title
    }
    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = resources.getString(title),
            message = reason.message(resources),
            okAction = action,
            cancelAction = CustomDialogDisplayer.Payload.DialogAction.noOp(resources.getString(R.string.common_cancel))
        )
    )
}
