package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationFailure
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun rebondValidationFailure(
    reason: RebondValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        RebondValidationFailure.CannotPayFee -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.common_not_enough_funds_message)
        }

        RebondValidationFailure.NotEnoughUnbondings -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.staking_rebond_insufficient_bondings)
        }

        RebondValidationFailure.ZeroAmount -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.common_zero_amount_error)
        }

        is RebondValidationFailure.NotEnoughBalanceToStayAboveED -> handleInsufficientBalanceCommission(
            reason,
            resourceManager
        )
    }
}
