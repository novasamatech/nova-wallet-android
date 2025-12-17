package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationFailure
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun bondMoreValidationFailure(
    reason: BondMoreValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        BondMoreValidationFailure.NotEnoughToPayFees, BondMoreValidationFailure.NotEnoughStakeable -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.choose_amount_error_too_big)
        }

        BondMoreValidationFailure.ZeroBond -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.common_zero_amount_error)
        }

        is BondMoreValidationFailure.NotEnoughFundToStayAboveED -> handleInsufficientBalanceCommission(
            reason,
            resourceManager
        )
    }
}
