package io.novafoundation.nova.feature_staking_impl.presentation.staking.balance

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.ManageStakingValidationFailure

fun manageStakingActionValidationFailure(
    reason: ManageStakingValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        is ManageStakingValidationFailure.ControllerRequired -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.staking_add_controller, reason.controllerAddress)
        }

        is ManageStakingValidationFailure.UnbondingRequestLimitReached -> {
            resourceManager.getString(R.string.staking_unbonding_limit_reached_title) to
                resourceManager.getString(R.string.staking_unbonding_limit_reached_message, reason.limit)
        }
        is ManageStakingValidationFailure.StashRequired -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.staking_stash_missing_message, reason.stashAddress)
        }
    }
}
