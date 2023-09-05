package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypePayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class StakingTypeAvailabilityValidation(
    private val availableStakingTypes: List<Chain.Asset.StakingType>,
    private val error: (EditingStakingTypePayload) -> EditingStakingTypeFailure,
) : Validation<EditingStakingTypePayload, EditingStakingTypeFailure> {

    override suspend fun validate(value: EditingStakingTypePayload): ValidationStatus<EditingStakingTypeFailure> {
        return if (availableStakingTypes.contains(value.stakingType)) {
            valid()
        } else {
            validationError(error(value))
        }
    }
}

fun ValidationSystemBuilder<EditingStakingTypePayload, EditingStakingTypeFailure>.stakingTypeAvailability(
    availableStakingTypes: List<Chain.Asset.StakingType>,
    errorFormatter: (EditingStakingTypePayload) -> EditingStakingTypeFailure,
) {
    validate(
        StakingTypeAvailabilityValidation(
            availableStakingTypes,
            errorFormatter,
        )
    )
}
