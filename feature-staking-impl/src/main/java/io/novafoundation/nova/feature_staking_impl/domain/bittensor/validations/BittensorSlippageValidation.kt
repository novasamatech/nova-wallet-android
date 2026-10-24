package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.postTradePrice

/**
 * The re-read market must still fill at the tighter of both limits.
 * A move past it is a warning whose alert offers the new rate; an unread market blocks the stake.
 */
class BittensorSlippageValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val quote = value.quote ?: return valid()
        val latestQuote = value.latestQuote ?: return validationError(BittensorStakeValidationFailure.QUOTE_UNAVAILABLE)
        val limitPrice = minOf(quote.limitPrice, latestQuote.limitPrice)
        return validOrWarning(latestQuote.postTradePrice() < limitPrice) { BittensorStakeValidationFailure.SlippageExceeded(quote, latestQuote) }
    }
}
