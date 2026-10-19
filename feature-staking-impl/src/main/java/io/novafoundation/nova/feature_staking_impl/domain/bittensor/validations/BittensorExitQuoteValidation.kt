package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot

class BittensorExitQuoteValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = if (value.quote.netuid.isRoot()) {
        valid()
    } else {
        val latest = value.latestQuote
        val limit = requireNotNull(value.quote.limitPrice)
        val postTradePrice = latest?.postTradePrice
        validOrError(
            latest != null && latest.netuid == value.quote.netuid && latest.amount == value.quote.amount &&
                latest.serviceFee == value.quote.serviceFee && latest.simulatedInput != null &&
                postTradePrice != null && postTradePrice > limit &&
                latest.estimatedTaoOut >= value.quote.minimumTaoOut
        ) { BittensorExitValidationFailure.QUOTE_CHANGED }
    }
}
