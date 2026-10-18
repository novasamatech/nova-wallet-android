package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

class BittensorExitHoldValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        return validOrError(
            value.preflight.rootHoldRemaining.isZero
        ) { BittensorExitValidationFailure.ROOT_LOCKED }
    }
}
