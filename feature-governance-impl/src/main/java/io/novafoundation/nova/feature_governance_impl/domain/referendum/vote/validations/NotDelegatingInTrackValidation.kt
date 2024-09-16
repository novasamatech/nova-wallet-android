package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting

class NotDelegatingInTrackValidation : VoteReferendumValidation {

    override suspend fun validate(value: VoteReferendaValidationPayload): ValidationStatus<VoteReferendumValidationFailure> {
        val isDelegating = value.trackVoting.any { it is Voting.Delegating }

        return isDelegating isFalseOrError {
            VoteReferendumValidationFailure.AlreadyDelegatingVotes
        }
    }
}

fun VoteReferendumValidationSystemBuilder.notDelegatingInTrack() {
    validate(NotDelegatingInTrackValidation())
}
