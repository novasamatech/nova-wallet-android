package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError

class PublicCrowdloanValidation : ContributeValidation {

    override suspend fun validate(value: ContributeValidationPayload): ValidationStatus<ContributeValidationFailure> {
        return validOrError(value.crowdloan.fundInfo.verifier == null) {
            ContributeValidationFailure.PrivateCrowdloanNotSupported
        }
    }
}
