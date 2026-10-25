package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot

/**
 * The sale alone must not move the pool price past the limit it is signed at.
 * A restored quote without a simulation has no post-trade price to judge; the latest quote is checked later.
 */
class BittensorExitSelfImpactValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = if (value.quote.netuid.isRoot() || value.quote.postTradePrice == null) {
        valid()
    } else {
        validOrError(value.quote.isFillable(atLimit = requireNotNull(value.signingLimit))) {
            BittensorExitValidationFailure.ORDER_TOO_LARGE
        }
    }
}
