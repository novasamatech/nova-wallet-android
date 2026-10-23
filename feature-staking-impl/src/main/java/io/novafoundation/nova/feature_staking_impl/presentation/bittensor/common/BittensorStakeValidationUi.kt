package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import java.math.BigInteger

fun BittensorStakeValidationFailure.title(resources: ResourceManager): String = when (this) {
    is BittensorStakeValidationFailure.BelowMinimumStake -> resources.getString(R.string.common_amount_low)
    else -> resources.getString(R.string.common_error_general_title)
}

/** [formatAmount] renders native-token planks; it is only used by failures that carry an amount. */
fun BittensorStakeValidationFailure.message(resources: ResourceManager, formatAmount: (BigInteger) -> CharSequence): String = when (this) {
    // As on iOS: the same wording for root and subnets, naming the amount the user has to enter.
    is BittensorStakeValidationFailure.BelowMinimumStake -> resources.getString(
        R.string.bittensor_validation_minimum_stake,
        formatAmount(requiredAmountInPlanks)
    )
    else -> resources.getString(messageRes())
}

private fun BittensorStakeValidationFailure.messageRes(): Int = when (this) {
    BittensorStakeValidationFailure.DELAYED_EXECUTION -> R.string.bittensor_multisig_unavailable
    BittensorStakeValidationFailure.NON_POSITIVE_AMOUNT -> R.string.common_enter_amount
    BittensorStakeValidationFailure.INSUFFICIENT_BALANCE -> R.string.common_error_not_enough_tokens
    BittensorStakeValidationFailure.FEE_RESERVE -> R.string.bittensor_validation_fee_reserve
    BittensorStakeValidationFailure.HOTKEY_NOT_REGISTERED -> R.string.bittensor_validation_hotkey
    BittensorStakeValidationFailure.SUBNET_STAKING_DISABLED -> R.string.bittensor_validation_staking_disabled
    BittensorStakeValidationFailure.COLDKEY_SWAP_PENDING -> R.string.bittensor_validation_coldkey_swap
    BittensorStakeValidationFailure.SAFE_MODE_ACTIVE -> R.string.bittensor_validation_safe_mode
    BittensorStakeValidationFailure.QUOTE_UNAVAILABLE -> R.string.bittensor_validation_quote_unavailable
    BittensorStakeValidationFailure.QUOTE_EXPIRED -> R.string.bittensor_validation_quote_expired
    BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED -> R.string.bittensor_validation_slippage
    BittensorStakeValidationFailure.HIGH_PRICE_IMPACT -> R.string.bittensor_validation_price_impact
    BittensorStakeValidationFailure.INACTIVE_VALIDATOR -> R.string.bittensor_inactive_validator_message
    is BittensorStakeValidationFailure.BelowMinimumStake -> R.string.bittensor_validation_minimum_stake
}

fun bittensorStakeValidationFailureUi(
    failure: ValidationStatus.NotValid<BittensorStakeValidationFailure>,
    actions: ValidationFlowActions<BittensorStakePreflightPayload>,
    resources: ResourceManager,
    formatAmount: (BigInteger) -> CharSequence,
    onOutcome: (StakingValidationOutcome) -> Unit = {},
    refreshQuote: () -> Unit
): TransformedFailure {
    val reason = failure.reason
    if (reason == BittensorStakeValidationFailure.INACTIVE_VALIDATOR) {
        return TransformedFailure.Custom(
            CustomDialogDisplayer.Payload(
                title = resources.getString(R.string.bittensor_inactive_validator_title),
                message = reason.message(resources, formatAmount),
                okAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_continue)) {
                    onOutcome(StakingValidationOutcome.PROCEEDED)
                    actions.revalidate { it.copy(inactiveValidatorAccepted = true) }
                },
                cancelAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_cancel)) {
                    onOutcome(
                        StakingValidationOutcome.CANCELLED
                    )
                }
            )
        )
    }
    if (reason == BittensorStakeValidationFailure.QUOTE_EXPIRED || reason == BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED) {
        return TransformedFailure.Custom(
            CustomDialogDisplayer.Payload(
                title = resources.getString(R.string.common_error_general_title),
                message = reason.message(resources, formatAmount),
                okAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.bittensor_review_updated_quote)) {
                    onOutcome(StakingValidationOutcome.PROCEEDED)
                    refreshQuote()
                },
                cancelAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_cancel)) {
                    onOutcome(
                        StakingValidationOutcome.CANCELLED
                    )
                }
            )
        )
    }
    if (reason != BittensorStakeValidationFailure.HIGH_PRICE_IMPACT) {
        onOutcome(StakingValidationOutcome.BLOCKED)
        return TransformedFailure.Default(reason.title(resources) to reason.message(resources, formatAmount))
    }

    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = resources.getString(R.string.bittensor_price_impact_title),
            message = reason.message(resources, formatAmount),
            okAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_continue)) {
                onOutcome(StakingValidationOutcome.PROCEEDED)
                actions.revalidate { it.copy(priceImpactAccepted = true) }
            },
            cancelAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_cancel)) {
                onOutcome(
                    StakingValidationOutcome.CANCELLED
                )
            }
        )
    )
}
