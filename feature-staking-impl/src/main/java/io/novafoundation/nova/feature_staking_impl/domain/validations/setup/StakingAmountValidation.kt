package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypePayload

class StakingAmountValidation(
    private val error: (EditingStakingTypePayload) -> EditingStakingTypeFailure,
) : Validation<EditingStakingTypePayload, EditingStakingTypeFailure> {

    override suspend fun validate(value: EditingStakingTypePayload): ValidationStatus<EditingStakingTypeFailure> {
        return if (value.minStake <= value.selectedAmount) {
            valid()
        } else {
            validationError(error(value))
        }
    }
}

fun ValidationSystemBuilder<EditingStakingTypePayload, EditingStakingTypeFailure>.stakingAmountValidation(
    errorFormatter: (EditingStakingTypePayload) -> EditingStakingTypeFailure,
) {
    validate(
        StakingAmountValidation(
            errorFormatter,
        )
    )
}
