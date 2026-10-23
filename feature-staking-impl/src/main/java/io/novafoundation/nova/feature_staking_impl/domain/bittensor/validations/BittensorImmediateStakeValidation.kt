package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.common.validation.ValidationStatus

class BittensorImmediateStakeValidation : Validation<Boolean, BittensorStakeValidationFailure> {

    override suspend fun validate(value: Boolean): ValidationStatus<BittensorStakeValidationFailure> = validOrError(value) {
        BittensorStakeValidationFailure.DELAYED_EXECUTION
    }
}
