package io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationFailure
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun redeemValidationFailure(
    reason: RedeemValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        RedeemValidationFailure.CannotPayFees -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.common_not_enough_funds_message)
        }

        is RedeemValidationFailure.NotEnoughBalanceToStayAboveED -> handleInsufficientBalanceCommission(
            reason,
            resourceManager
        )
    }
}
