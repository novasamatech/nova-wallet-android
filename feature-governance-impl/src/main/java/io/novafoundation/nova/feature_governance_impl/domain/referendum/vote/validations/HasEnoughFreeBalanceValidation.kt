package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError

class HasEnoughFreeBalanceValidation : VoteReferendumValidation {

    override suspend fun validate(value: VoteReferendumValidationPayload): ValidationStatus<VoteReferendumValidationFailure> {
        val leftToVote = value.asset.free - value.fee

        return (leftToVote >= value.voteAmount) isTrueOrError {
            VoteReferendumValidationFailure.AmountIsTooBig(
                chainAsset = value.asset.token.configuration,
                availableToVote = leftToVote
            )
        }
    }
}


fun VoteReferendumValidationSystemBuilder.hasEnoughFreeBalance() {
    validate(HasEnoughFreeBalanceValidation())
}
