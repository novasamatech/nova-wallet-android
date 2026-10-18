package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

class BittensorExitSafeModeValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = validOrError(
        !value.preflight.safeModeActive
    ) { BittensorExitValidationFailure.SAFE_MODE_ACTIVE }
}
