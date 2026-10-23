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

/** Formatted TAO values of a remainder the network would sweep, see `BittensorExitDustRemainder`. */
data class BittensorExitDustRemainderUi(val remainder: String, val minimum: String)

fun bittensorExitValidationFailureUi(
    reason: BittensorExitValidationFailure,
    resources: ResourceManager,
    isRoot: Boolean,
    dustRemainder: BittensorExitDustRemainderUi?,
    unstakeAll: () -> Unit,
    acceptRemainder: () -> Unit,
    refreshQuote: () -> Unit,
    acceptImpact: () -> Unit,
    onOutcome: (StakingValidationOutcome) -> Unit = {}
): TransformedFailure {
    remainderFailureUi(reason, resources, isRoot, dustRemainder, unstakeAll, acceptRemainder, onOutcome)?.let { return it }
    val action = when (reason) {
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

private fun exitAllLabel(isRoot: Boolean) = if (isRoot) R.string.staking_unstake_all else R.string.bittensor_exit_sell_all

// iOS SubtensorErrorPresentable.presentDustRemainderWarning / presentRemainderLocked.
private fun remainderFailureUi(
    reason: BittensorExitValidationFailure,
    resources: ResourceManager,
    isRoot: Boolean,
    dustRemainder: BittensorExitDustRemainderUi?,
    unstakeAll: () -> Unit,
    acceptRemainder: () -> Unit,
    onOutcome: (StakingValidationOutcome) -> Unit
): TransformedFailure? = when (reason) {
    BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL -> TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = resources.getString(R.string.bittensor_exit_dust_remainder_title),
            message = dustRemainder?.let { resources.getString(R.string.bittensor_exit_dust_remainder_message, it.remainder, it.minimum) }
                ?: reason.message(resources),
            okAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_proceed)) {
                onOutcome(StakingValidationOutcome.PROCEEDED)
                acceptRemainder()
            },
            cancelAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_cancel)) {
                onOutcome(StakingValidationOutcome.CANCELLED)
            },
            neutralAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(exitAllLabel(isRoot))) {
                onOutcome(StakingValidationOutcome.PROCEEDED)
                unstakeAll()
            }
        )
    )
    BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM -> {
        onOutcome(StakingValidationOutcome.BLOCKED)
        val message = if (isRoot) R.string.bittensor_exit_remainder_locked_unstake_message else R.string.bittensor_exit_remainder_locked_sale_message
        TransformedFailure.Default(
            resources.getString(R.string.bittensor_exit_remainder_locked_title) to
                (dustRemainder?.let { resources.getString(message, it.remainder, it.minimum) } ?: reason.message(resources))
        )
    }
    else -> null
}
