package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationFailure

fun welcomeStakingValidationFailure(
    reason: WelcomeStakingValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage = with(resourceManager) {
    when (reason) {
        WelcomeStakingValidationFailure.MAX_NOMINATORS_REACHED -> {
            getString(R.string.staking_max_nominators_reached_title) to getString(R.string.staking_max_nominators_reached_message)
        }
    }
}

fun mainStakingValidationFailure(
    reason: StakeActionsValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage = with(resourceManager) {
    when(reason) {
        is StakeActionsValidationFailure.ControllerRequired -> {
            getString(R.string.common_error_general_title) to
                getString(R.string.staking_add_controller, reason.controllerAddress)
        }
    }
}
