package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationPayload

class BittensorClaimProfitValidation : BittensorClaimValidation {

    override suspend fun validate(value: BittensorClaimValidationPayload) = validOrWarning(
        value.current == null || value.uneconomicClaimAccepted || value.current.redeemable > value.fee
    ) { BittensorClaimValidationFailure.UNECONOMIC_CLAIM }
}
