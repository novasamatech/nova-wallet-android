package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationPayload

class BittensorClaimPendingValidation : BittensorClaimValidation {

    override suspend fun validate(value: BittensorClaimValidationPayload) = validOrError(!value.claimPending) {
        BittensorClaimValidationFailure.CLAIM_PENDING
    }
}
