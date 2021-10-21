package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationFailure

fun bondMoreValidationFailure(
    reason: BondMoreValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        BondMoreValidationFailure.NOT_ENOUGH_TO_PAY_FEES -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.common_not_enough_funds_message)
        }

        BondMoreValidationFailure.ZERO_BOND -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.staking_zero_bond_error)
        }
    }
}
