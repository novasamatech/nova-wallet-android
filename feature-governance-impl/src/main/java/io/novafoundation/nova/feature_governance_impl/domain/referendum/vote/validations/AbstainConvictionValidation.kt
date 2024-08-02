package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

class AbstainConvictionValidation : VoteReferendumValidation {

    override suspend fun validate(value: VoteReferendumValidationPayload): ValidationStatus<VoteReferendumValidationFailure> {
        val isAbstainVote = value.voteType == VoteType.ABSTAIN
        val isConvictionNone = value.conviction == Conviction.None

        if (isAbstainVote && !isConvictionNone) {
            return validationError(VoteReferendumValidationFailure.AbstainInvalidConviction)
        }

        return valid()
    }
}

fun VoteReferendumValidationSystemBuilder.abstainConvictionValid() {
    validate(AbstainConvictionValidation())
}
