package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure

fun BittensorStakeValidationFailure.message(resources: ResourceManager): String = resources.getString(
    when (this) {
        BittensorStakeValidationFailure.DELAYED_EXECUTION -> R.string.bittensor_multisig_unavailable
        BittensorStakeValidationFailure.NON_POSITIVE_AMOUNT -> R.string.common_enter_amount
        BittensorStakeValidationFailure.INSUFFICIENT_BALANCE -> R.string.common_error_not_enough_tokens
        BittensorStakeValidationFailure.FEE_RESERVE -> R.string.bittensor_validation_fee_reserve
        BittensorStakeValidationFailure.BELOW_MINIMUM_STAKE -> R.string.bittensor_validation_minimum_stake
        BittensorStakeValidationFailure.HOTKEY_NOT_REGISTERED -> R.string.bittensor_validation_hotkey
        BittensorStakeValidationFailure.SUBNET_STAKING_DISABLED -> R.string.bittensor_validation_staking_disabled
        BittensorStakeValidationFailure.COLDKEY_SWAP_PENDING -> R.string.bittensor_validation_coldkey_swap
        BittensorStakeValidationFailure.SAFE_MODE_ACTIVE -> R.string.bittensor_validation_safe_mode
        BittensorStakeValidationFailure.QUOTE_UNAVAILABLE -> R.string.bittensor_validation_quote_unavailable
        BittensorStakeValidationFailure.QUOTE_EXPIRED -> R.string.bittensor_validation_quote_expired
        BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED -> R.string.bittensor_validation_slippage
        BittensorStakeValidationFailure.HIGH_PRICE_IMPACT -> R.string.bittensor_validation_price_impact
        BittensorStakeValidationFailure.INACTIVE_VALIDATOR -> R.string.bittensor_inactive_validator_message
    }
)

fun bittensorStakeValidationFailureUi(
    failure: ValidationStatus.NotValid<BittensorStakeValidationFailure>,
    actions: ValidationFlowActions<BittensorStakePreflightPayload>,
    resources: ResourceManager,
    refreshQuote: () -> Unit
): TransformedFailure {
    val reason = failure.reason
    if (reason == BittensorStakeValidationFailure.INACTIVE_VALIDATOR) {
        return TransformedFailure.Custom(
            CustomDialogDisplayer.Payload(
                title = resources.getString(R.string.bittensor_inactive_validator_title),
                message = reason.message(resources),
                okAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_continue)) {
                    actions.revalidate { it.copy(inactiveValidatorAccepted = true) }
                },
                cancelAction = CustomDialogDisplayer.Payload.DialogAction.noOp(resources.getString(R.string.common_cancel))
            )
        )
    }
    if (reason == BittensorStakeValidationFailure.QUOTE_EXPIRED || reason == BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED) {
        return TransformedFailure.Custom(
            CustomDialogDisplayer.Payload(
                title = resources.getString(R.string.common_error_general_title),
                message = reason.message(resources),
                okAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.bittensor_review_updated_quote), refreshQuote),
                cancelAction = CustomDialogDisplayer.Payload.DialogAction.noOp(resources.getString(R.string.common_cancel))
            )
        )
    }
    if (reason != BittensorStakeValidationFailure.HIGH_PRICE_IMPACT) {
        return TransformedFailure.Default(resources.getString(R.string.common_error_general_title) to reason.message(resources))
    }

    return TransformedFailure.Custom(
        CustomDialogDisplayer.Payload(
            title = resources.getString(R.string.bittensor_price_impact_title),
            message = reason.message(resources),
            okAction = CustomDialogDisplayer.Payload.DialogAction(resources.getString(R.string.common_continue)) {
                actions.revalidate { it.copy(priceImpactAccepted = true) }
            },
            cancelAction = CustomDialogDisplayer.Payload.DialogAction.noOp(resources.getString(R.string.common_cancel))
        )
    )
}
