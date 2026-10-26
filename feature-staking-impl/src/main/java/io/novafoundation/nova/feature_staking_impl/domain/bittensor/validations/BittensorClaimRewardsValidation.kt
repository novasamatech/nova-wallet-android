package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationPayload
import java.math.BigInteger

class BittensorClaimRewardsValidation : BittensorClaimValidation {

    override suspend fun validate(value: BittensorClaimValidationPayload) = validOrError(
        value.current == null || (value.reviewed.redeemable - value.current.redeemable) * PERCENT_DENOMINATOR <= value.reviewed.redeemable
    ) { BittensorClaimValidationFailure.REWARDS_DECREASED }

    private companion object {

        val PERCENT_DENOMINATOR: BigInteger = BigInteger.valueOf(100)
    }
}
