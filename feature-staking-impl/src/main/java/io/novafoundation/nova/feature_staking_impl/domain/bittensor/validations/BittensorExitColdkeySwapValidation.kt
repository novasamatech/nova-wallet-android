package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

class BittensorExitColdkeySwapValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = validOrError(
        !value.preflight.coldkeySwapPending
    ) { BittensorExitValidationFailure.COLDKEY_SWAP_PENDING }
}
