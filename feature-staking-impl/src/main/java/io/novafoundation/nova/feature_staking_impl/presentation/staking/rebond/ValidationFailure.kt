package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationFailure

fun rebondValidationFailure(
    reason: RebondValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        RebondValidationFailure.CANNOT_PAY_FEE -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.common_not_enough_funds_message)
        }

        RebondValidationFailure.NOT_ENOUGH_UNBONDINGS -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.staking_rebond_insufficient_bondings)
        }

        RebondValidationFailure.ZERO_AMOUNT -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.staking_zero_bond_error)
        }
    }
}
