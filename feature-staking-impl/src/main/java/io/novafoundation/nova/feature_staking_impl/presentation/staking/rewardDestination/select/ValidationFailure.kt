package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.select

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationFailure
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun rewardDestinationValidationFailure(
    resourceManager: ResourceManager,
    failure: RewardDestinationValidationFailure
): TitleAndMessage = when (failure) {
    is RewardDestinationValidationFailure.MissingController -> {
        resourceManager.getString(R.string.common_error_general_title) to
            resourceManager.getString(R.string.staking_add_controller, failure.controllerAddress)
    }

    RewardDestinationValidationFailure.CannotPayFees -> {
        resourceManager.getString(R.string.common_not_enough_funds_title) to
            resourceManager.getString(R.string.common_not_enough_funds_message)
    }

    is RewardDestinationValidationFailure.NotEnoughBalanceToStayAboveED -> handleInsufficientBalanceCommission(
        failure,
        resourceManager
    )
}
