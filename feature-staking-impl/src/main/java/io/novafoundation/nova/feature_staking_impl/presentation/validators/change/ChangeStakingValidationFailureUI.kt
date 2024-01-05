package io.novafoundation.nova.feature_staking_impl.presentation.validators.change

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.ChangeStackingValidationFailure

fun mapAddEvmTokensValidationFailureToUI(
    resourceManager: ResourceManager,
    failure: ChangeStackingValidationFailure
): TitleAndMessage {
    return when (failure) {
        ChangeStackingValidationFailure.NO_ACCESS_TO_CONTROLLER_ACCOUNT -> {
            resourceManager.getString(R.string.stacking_no_access_to_controller_account_title) to
                resourceManager.getString(R.string.stacking_no_access_to_controller_account_message)
        }
    }
}
