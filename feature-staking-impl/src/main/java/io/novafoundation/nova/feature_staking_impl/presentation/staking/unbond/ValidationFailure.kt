package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.formatWith

fun unbondValidationFailure(
    reason: UnbondValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        is UnbondValidationFailure.BondedWillCrossExistential -> reason.formatWith(resourceManager)

        UnbondValidationFailure.CannotPayFees -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.common_not_enough_funds_message)
        }

        UnbondValidationFailure.NotEnoughBonded -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.staking_unbond_too_big)
        }

        is UnbondValidationFailure.UnbondLimitReached -> {
            resourceManager.getString(R.string.staking_unbonding_limit_reached_title) to
                resourceManager.getString(R.string.staking_unbonding_limit_reached_message, reason.limit)
        }

        UnbondValidationFailure.ZeroUnbond -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.common_zero_amount_error)
        }
    }
}

fun unbondPayloadAutoFix(payload: UnbondValidationPayload, reason: UnbondValidationFailure) = when (reason) {
    is UnbondValidationFailure.BondedWillCrossExistential -> payload.copy(amount = reason.errorContext.wholeAmount)
    else -> payload
}
