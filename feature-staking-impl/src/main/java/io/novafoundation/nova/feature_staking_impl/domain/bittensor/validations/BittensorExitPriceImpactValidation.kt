package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import java.math.BigInteger

class BittensorExitPriceImpactValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = value.latestQuote?.let { quote ->
        val spot = requireNotNull(quote.spotPrice)
        val executionPrice = requireNotNull(quote.postTradePrice)
        validOrWarning(value.priceImpactAccepted || (spot - executionPrice).abs() * BigInteger.valueOf(100) <= spot) {
            BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH
        }
    } ?: valid()
}
