package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload

private const val QUOTE_LIFETIME_MILLIS = 15_000L

class BittensorQuoteFreshnessValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val quote = value.latestQuote ?: value.quote ?: return valid()
        val ageMillis = value.checkedAtMillis - quote.capturedAtMillis
        return validOrError(ageMillis in 0..QUOTE_LIFETIME_MILLIS) { BittensorStakeValidationFailure.QUOTE_EXPIRED }
    }
}
