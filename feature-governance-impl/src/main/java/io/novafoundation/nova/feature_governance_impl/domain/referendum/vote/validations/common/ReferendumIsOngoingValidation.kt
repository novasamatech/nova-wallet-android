package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId

class ReferendumIsOngoingValidation<P : VoteValidationPayload, F>(
    private val failure: (ReferendumId) -> F
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        val finishedReferendum = value.onChainReferenda.firstOrNull { it.status !is OnChainReferendumStatus.Ongoing }

        if (finishedReferendum == null) {
            return valid()
        } else {
            return validationError(failure(finishedReferendum.id))
        }
    }
}
