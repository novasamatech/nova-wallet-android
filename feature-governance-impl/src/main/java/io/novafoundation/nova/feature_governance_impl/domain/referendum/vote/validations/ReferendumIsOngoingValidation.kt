package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus

class ReferendumIsOngoingValidation : VoteReferendumValidation {

    override suspend fun validate(value: VoteReferendaValidationPayload): ValidationStatus<VoteReferendumValidationFailure> {
        val isOngoing = value.onChainReferenda.all { it.status is OnChainReferendumStatus.Ongoing }

        return isOngoing isTrueOrError {
            VoteReferendumValidationFailure.ReferendumCompleted
        }
    }
}

fun VoteReferendumValidationSystemBuilder.referendumIsOngoing() {
    validate(ReferendumIsOngoingValidation())
}
