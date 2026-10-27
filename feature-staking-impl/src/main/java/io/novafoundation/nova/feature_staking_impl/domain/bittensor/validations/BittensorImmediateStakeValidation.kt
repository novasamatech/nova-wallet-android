package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWalletSupport

class BittensorImmediateStakeValidation : Validation<BittensorWalletSupport, BittensorStakeValidationFailure> {

    override suspend fun validate(value: BittensorWalletSupport): ValidationStatus<BittensorStakeValidationFailure> = when (value) {
        BittensorWalletSupport.SUPPORTED -> valid()
        BittensorWalletSupport.PROXIED_NOT_SUPPORTED -> validationError(BittensorStakeValidationFailure.PROXIED_NOT_SUPPORTED)
        BittensorWalletSupport.DELAYED_EXECUTION -> validationError(BittensorStakeValidationFailure.DELAYED_EXECUTION)
    }
}
