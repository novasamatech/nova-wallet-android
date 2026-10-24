package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import java.math.BigInteger

/** iOS `priceImpactAcceptable`: judged on the shown quote, before the re-read one is checked. */
class BittensorExitPriceImpactValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = value.quote.let { quote ->
        val spot = quote.spotPrice
        val executionPrice = quote.postTradePrice
        if (spot == null || executionPrice == null) {
            valid()
        } else {
            validOrWarning(value.priceImpactAccepted || (spot - executionPrice).abs() * BigInteger.valueOf(100) <= spot) {
                BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH
            }
        }
    }
}
