package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFreeBalanceError

fun handleVoteReferendumValidationFailure(failure: VoteReferendumValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (failure) {
        is VoteReferendumValidationFailure.NotEnoughToPayFees -> handleNotEnoughFeeError(failure, resourceManager)

        VoteReferendumValidationFailure.AlreadyDelegatingVotes -> {
            resourceManager.getString(R.string.refrendum_vote_already_delegating_title) to
                resourceManager.getString(R.string.refrendum_vote_already_delegating_message)
        }

        is VoteReferendumValidationFailure.AmountIsTooBig -> handleNotEnoughFreeBalanceError(
            error = failure,
            resourceManager = resourceManager,
            descriptionFormat = R.string.refrendum_vote_not_enough_available_message
        )

        is VoteReferendumValidationFailure.MaxTrackVotesReached -> {
            resourceManager.getString(R.string.refrendum_vote_max_votes_reached_title) to
                resourceManager.getString(R.string.refrendum_vote_max_votes_reached_message, failure.max.format())
        }

        VoteReferendumValidationFailure.ReferendumCompleted -> {
            resourceManager.getString(R.string.refrendum_vote_already_completed_title) to
                resourceManager.getString(R.string.refrendum_vote_already_completed_message)
        }
    }
}
