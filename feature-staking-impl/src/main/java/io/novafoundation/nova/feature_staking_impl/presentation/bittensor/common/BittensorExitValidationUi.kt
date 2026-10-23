package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure

/** Formatted TAO values of a remainder the network would sweep, see `BittensorExitDustRemainder`. */
data class BittensorExitDustRemainderUi(val remainder: String, val minimum: String)

/** A network fee the exit cannot pay; [missing] is set when the free balance has to cover the fee and the existential deposit. */
data class BittensorExitFeeShortfallUi(val required: String, val balance: String, val missing: String?)

/** The TAO that [unit] sold for at the shown and at the re-read quote. */
data class BittensorExitRateUpdateUi(val unit: String, val old: String, val new: String)

/** Formatted values the iOS alerts name for the exit failures. */
class BittensorExitFailureValues(
    val minimum: String,
    val maximum: String,
    val priceImpact: String,
    val holdEta: String?,
    val feeShortfall: BittensorExitFeeShortfallUi,
    val dustRemainder: BittensorExitDustRemainderUi?,
    val rateUpdate: BittensorExitRateUpdateUi?
)

// iOS SubtensorErrorPresentable and BaseErrorPresentable; failures without an iOS alert keep their own message.
private fun BittensorExitValidationFailure.titleAndMessage(
    resources: ResourceManager,
    isRoot: Boolean,
    values: BittensorExitFailureValues
): TitleAndMessage {
    val fee = values.feeShortfall
    fun remainderMessage(format: Int) = values.dustRemainder?.let { resources.getString(format, it.remainder, it.minimum) }
        ?: resources.getString(R.string.bittensor_exit_remainder_below_minimum)
    return when (this) {
        BittensorExitValidationFailure.COLDKEY_SWAP_PENDING ->
            resources.getString(R.string.bittensor_validation_coldkey_swap_title) to resources.getString(R.string.bittensor_validation_coldkey_swap)
        BittensorExitValidationFailure.SAFE_MODE_ACTIVE ->
            resources.getString(R.string.bittensor_validation_safe_mode_title) to resources.getString(R.string.bittensor_validation_safe_mode)
        BittensorExitValidationFailure.SUBNET_TRADING_DISABLED ->
            resources.getString(R.string.bittensor_validation_staking_disabled_title) to resources.getString(R.string.bittensor_validation_staking_disabled)
        BittensorExitValidationFailure.ORDER_TOO_LARGE ->
            resources.getString(R.string.bittensor_exit_self_impact_title) to resources.getString(R.string.bittensor_exit_self_impact_message)
        BittensorExitValidationFailure.ROOT_LOCKED -> {
            val message = values.holdEta?.let { resources.getString(R.string.bittensor_exit_locked_message, it) }
            resources.getString(R.string.bittensor_exit_locked_title) to (message ?: resources.getString(R.string.bittensor_validation_root_locked))
        }
        BittensorExitValidationFailure.INSUFFICIENT_STAKE ->
            resources.getString(R.string.common_amount_too_big) to resources.getString(R.string.bittensor_exit_exceeds_available, values.maximum)
        BittensorExitValidationFailure.FULL_POSITION_CHANGED -> resources.getString(R.string.bittensor_exit_position_changed_title) to null
        BittensorExitValidationFailure.BELOW_MINIMUM ->
            resources.getString(R.string.common_amount_low) to resources.getString(R.string.bittensor_exit_below_minimum, values.minimum)
        BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM -> {
            val format = if (isRoot) R.string.bittensor_exit_remainder_locked_unstake_message else R.string.bittensor_exit_remainder_locked_sale_message
            resources.getString(R.string.bittensor_exit_remainder_locked_title) to remainderMessage(format)
        }
        BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL ->
            resources.getString(R.string.bittensor_exit_dust_remainder_title) to remainderMessage(R.string.bittensor_exit_dust_remainder_message)
        BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE -> if (fee.missing != null) {
            resources.getString(R.string.common_amount_low) to
                resources.getString(R.string.wallet_send_insufficient_balance_commission, fee.required, fee.balance, fee.missing)
        } else {
            resources.getString(R.string.error_not_enough_to_pay_fee_title) to
                resources.getString(R.string.bittensor_exit_not_enough_fee_message, fee.required, fee.balance)
        }
        BittensorExitValidationFailure.FEE_SOURCE_CHANGED ->
            resources.getString(R.string.common_error_general_title) to resources.getString(R.string.bittensor_exit_fee_source_changed)
        BittensorExitValidationFailure.QUOTE_CHANGED -> resources.getString(R.string.swap_rate_was_updated_failure_title) to
            values.rateUpdate?.let { resources.getString(R.string.swap_rate_was_updated_failure_message, it.unit, it.old, it.new) }
        BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH ->
            resources.getString(R.string.bittensor_price_impact_title) to resources.getString(R.string.bittensor_price_impact_message, values.priceImpact)
    }
}

/** [acceptRate] is null on the review, where a moved price raises a banner instead of an alert (iOS confirm). */
fun bittensorExitValidationFailureUi(
    reason: BittensorExitValidationFailure,
    resources: ResourceManager,
    isRoot: Boolean,
    values: BittensorExitFailureValues,
    unstakeAll: (() -> Unit)?,
    acceptRemainder: () -> Unit,
    acceptRate: (() -> Unit)?,
    acceptImpact: () -> Unit,
    onOutcome: (StakingValidationOutcome) -> Unit = {}
): TransformedFailure? {
    val (title, message) = reason.titleAndMessage(resources, isRoot, values)
    fun proceed(label: Int, action: () -> Unit) = CustomDialogDisplayer.Payload.DialogAction(resources.getString(label)) {
        onOutcome(StakingValidationOutcome.PROCEEDED)
        action()
    }
    val cancel = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_cancel)) {
        onOutcome(StakingValidationOutcome.CANCELLED)
    }
    val dialog = when (reason) {
        // iOS presentDustRemainderWarning: "Unstake all" for sales as well, offered only when given.
        BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL -> CustomDialogDisplayer.Payload(
            title = title,
            message = message,
            okAction = proceed(R.string.common_proceed, acceptRemainder),
            cancelAction = cancel,
            neutralAction = unstakeAll?.let { proceed(R.string.staking_unstake_all, it) }
        )
        // iOS presentRateUpdated: Confirm takes the new rate.
        BittensorExitValidationFailure.QUOTE_CHANGED -> acceptRate?.let {
            CustomDialogDisplayer.Payload(title, message, okAction = proceed(R.string.common_confirm, it), cancelAction = cancel)
        } ?: return null
        BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH ->
            CustomDialogDisplayer.Payload(title, message, okAction = proceed(R.string.common_proceed, acceptImpact), cancelAction = cancel)
        else -> {
            onOutcome(StakingValidationOutcome.BLOCKED)
            return TransformedFailure.Default(title to message)
        }
    }
    return TransformedFailure.Custom(dialog)
}
