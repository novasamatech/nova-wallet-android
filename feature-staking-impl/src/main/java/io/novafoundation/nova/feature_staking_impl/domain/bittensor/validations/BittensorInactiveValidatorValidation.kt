package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload

class BittensorInactiveValidatorValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> =
        validOrWarning(!value.inactiveValidator || value.inactiveValidatorAccepted) {
            BittensorStakeValidationFailure.INACTIVE_VALIDATOR
        }
}
