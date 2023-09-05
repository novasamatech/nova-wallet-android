package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypePayload
import java.math.BigInteger

class StakingAmountValidation(
    private val singleStakingProperties: SingleStakingProperties,
    private val amount: (EditingStakingTypePayload) -> BigInteger,
    private val error: (EditingStakingTypePayload) -> EditingStakingTypeFailure,
) : Validation<EditingStakingTypePayload, EditingStakingTypeFailure> {

    override suspend fun validate(value: EditingStakingTypePayload): ValidationStatus<EditingStakingTypeFailure> {
        return if (singleStakingProperties.minStake() <= amount(value)) {
            valid()
        } else {
            validationError(error(value))
        }
    }
}

fun ValidationSystemBuilder<EditingStakingTypePayload, EditingStakingTypeFailure>.stakingAmountValidation(
    singleStakingProperties: SingleStakingProperties,
    amount: (EditingStakingTypePayload) -> BigInteger,
    errorFormatter: (EditingStakingTypePayload) -> EditingStakingTypeFailure,
) {
    validate(
        StakingAmountValidation(
            singleStakingProperties,
            amount,
            errorFormatter,
        )
    )
}
