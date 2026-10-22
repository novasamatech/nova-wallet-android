package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure

fun BittensorExitValidationFailure.message(resources: ResourceManager): String = resources.getString(
    when (this) {
        BittensorExitValidationFailure.COLDKEY_SWAP_PENDING -> R.string.bittensor_validation_coldkey_swap
        BittensorExitValidationFailure.SAFE_MODE_ACTIVE -> R.string.bittensor_validation_safe_mode
        BittensorExitValidationFailure.SUBNET_TRADING_DISABLED -> R.string.bittensor_validation_staking_disabled
        BittensorExitValidationFailure.ROOT_LOCKED -> R.string.bittensor_validation_root_locked
        BittensorExitValidationFailure.INSUFFICIENT_STAKE -> R.string.common_error_not_enough_tokens
        BittensorExitValidationFailure.FULL_POSITION_CHANGED -> R.string.bittensor_exit_position_changed
        BittensorExitValidationFailure.BELOW_MINIMUM -> R.string.bittensor_exit_below_minimum
        BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM -> R.string.bittensor_exit_remainder_below_minimum
        BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL -> R.string.bittensor_exit_remainder_below_minimum
        BittensorExitValidationFailure.QUOTE_CHANGED -> R.string.bittensor_validation_slippage
        BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH -> R.string.bittensor_validation_price_impact
        BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE -> R.string.bittensor_validation_fee_reserve
        BittensorExitValidationFailure.FEE_SOURCE_CHANGED -> R.string.bittensor_exit_fee_source_changed
    }
)

fun bittensorExitValidationFailureUi(
    reason: BittensorExitValidationFailure,
    resources: ResourceManager,
    unstakeAll: () -> Unit,
    refreshQuote: () -> Unit,
    acceptImpact: () -> Unit,
    onOutcome: (StakingValidationOutcome) -> Unit = {}
): TransformedFailure {
    val action = when (reason) {
        BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL ->
            CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.staking_unstake_all)) {
                onOutcome(StakingValidationOutcome.PROCEEDED)
                unstakeAll()
            }
        BittensorExitValidationFailure.QUOTE_CHANGED ->
            CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.bittensor_review_updated_quote)) {
                onOutcome(StakingValidationOutcome.PROCEEDED)
                refreshQuote()
            }
        BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH ->
            CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_continue)) {
                onOutcome(StakingValidationOutcome.PROCEEDED)
                acceptImpact()
            }
        else -> {
            onOutcome(StakingValidationOutcome.BLOCKED)
            return TransformedFailure.Default(resources.getString(R.string.common_error_general_title) to reason.message(resources))
        }
    }
    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = resources.getString(
                if (reason == BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH) {
                    R.string.bittensor_price_impact_title
                } else {
                    R.string.common_error_general_title
                }
            ),
            message = reason.message(resources),
            okAction = action,
            cancelAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_cancel)) {
                onOutcome(
                    StakingValidationOutcome.CANCELLED
                )
            }
        )
    )
}
