package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload

class BittensorQuoteTermsValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val reviewedQuote = value.quote ?: return valid()
        val latestQuote = value.latestQuote ?: return valid()
        val sameOperation = latestQuote.subnetId == reviewedQuote.subnetId &&
            latestQuote.totalTaoInPlanks == reviewedQuote.totalTaoInPlanks &&
            latestQuote.serviceFeeTaoInPlanks == reviewedQuote.serviceFeeTaoInPlanks

        return validOrError(sameOperation) { BittensorStakeValidationFailure.QUOTE_EXPIRED }
    }
}
