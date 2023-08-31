package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.direct.EditingStakingTypeFailure

class ValidatedStakingTypeDetails(
    val validationStatus: ValidationStatus<EditingStakingTypeFailure>?,
    val stakingTypeDetails: StakingTypeDetails
) {
    val isAvailable = validationStatus is ValidationStatus.Valid
}
