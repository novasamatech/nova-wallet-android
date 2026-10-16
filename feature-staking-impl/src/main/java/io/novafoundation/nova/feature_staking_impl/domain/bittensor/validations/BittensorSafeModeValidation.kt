package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload

class BittensorSafeModeValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        return validOrError(!value.preflight.safeModeActive) { BittensorStakeValidationFailure.SAFE_MODE_ACTIVE }
    }
}
