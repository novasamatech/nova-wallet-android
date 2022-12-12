package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationFailure.NOT_ENOUGH_STAKEABLE
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationFailure.NOT_ENOUGH_TO_PAY_FEES
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationFailure.ZERO_BOND

fun bondMoreValidationFailure(
    reason: BondMoreValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        NOT_ENOUGH_TO_PAY_FEES, NOT_ENOUGH_STAKEABLE -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.choose_amount_error_too_big)
        }

        ZERO_BOND -> {
            resourceManager.getString(R.string.common_error_general_title) to
                resourceManager.getString(R.string.common_zero_amount_error)
        }
    }
}
