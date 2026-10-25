package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload

/** Judged on the shown quote, before the re-read one is checked. */
class BittensorExitPriceImpactValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload): ValidationStatus<BittensorExitValidationFailure> {
        val priceImpact = value.quote.priceImpact ?: return valid()
        return validOrWarning(value.priceImpactAccepted || !priceImpact.isHigh) { BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH }
    }
}
