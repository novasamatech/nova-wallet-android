package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting

class NotDelegatingInTrackValidation<P : VoteValidationPayload, F>(
    private val failure: () -> F
) : Validation<P, F> {

    override suspend fun validate(value: P): ValidationStatus<F> {
        val isDelegating = value.trackVoting.any { it is Voting.Delegating }

        return isDelegating isFalseOrError {
            failure()
        }
    }
}
