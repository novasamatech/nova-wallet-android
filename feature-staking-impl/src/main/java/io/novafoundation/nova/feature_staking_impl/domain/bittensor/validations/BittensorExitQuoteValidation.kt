package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot

/**
 * The re-read quote must still fill at the tighter of its own and the acknowledged limit.
 * Its service fee follows the output, so it may differ; the signature takes the re-read quote as it is.
 */
class BittensorExitQuoteValidation : BittensorExitValidation {

    override suspend fun validate(value: BittensorExitValidationPayload) = if (value.quote.netuid.isRoot()) {
        valid()
    } else {
        val latest = value.latestQuote
        val limit = requireNotNull(value.acknowledgedLimit ?: value.quote.limitPrice)
        validOrWarning(
            latest != null && latest.netuid == value.quote.netuid && latest.amount == value.quote.amount &&
                latest.isFillable(atLimit = latest.tighterLimit(than = limit))
        ) { BittensorExitValidationFailure.QUOTE_CHANGED }
    }
}
