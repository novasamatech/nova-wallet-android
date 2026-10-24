package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.postTradePrice

/** The checked quote must fill at its own limit, otherwise the order itself is too large. */
class BittensorOrderToleranceValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val quote = value.quote ?: return valid()
        return validOrError(quote.postTradePrice() < quote.limitPrice) { BittensorStakeValidationFailure.SELF_IMPACT }
    }
}
