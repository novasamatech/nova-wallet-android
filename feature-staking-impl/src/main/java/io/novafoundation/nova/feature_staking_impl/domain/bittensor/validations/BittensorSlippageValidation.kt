package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.postTradePrice

class BittensorSlippageValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val quote = value.quote ?: return valid()
        val latestQuote = value.latestQuote ?: quote
        val limitPrice = minOf(quote.limitPrice, latestQuote.limitPrice)
        return validOrError(latestQuote.postTradePrice() < limitPrice) { BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED }
    }
}
