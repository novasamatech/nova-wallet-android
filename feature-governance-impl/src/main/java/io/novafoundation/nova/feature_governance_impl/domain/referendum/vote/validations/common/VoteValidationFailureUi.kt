package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.common.validation.TransformedFailure.Default
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFreeBalanceError

fun handleAlreadyDelegatingVotes(
    resourceManager: ResourceManager
): TransformedFailure {
    return Default(
        resourceManager.getString(R.string.refrendum_vote_already_delegating_title) to
            resourceManager.getString(R.string.refrendum_vote_already_delegating_message)
    )
}

fun handleAmountIsTooBig(
    resourceManager: ResourceManager,
    failure: VoteValidationFailure.AmountIsTooBig
): Default {
    return Default(
        handleNotEnoughFreeBalanceError(
            error = failure,
            resourceManager = resourceManager,
            descriptionFormat = R.string.refrendum_vote_not_enough_available_message
        )
    )
}

fun handleMaxTrackVotesReached(
    resourceManager: ResourceManager,
    failure: VoteValidationFailure.MaxTrackVotesReached
): TransformedFailure {
    return Default(
        resourceManager.getString(R.string.refrendum_vote_max_votes_reached_title) to
            resourceManager.getString(R.string.refrendum_vote_max_votes_reached_message, failure.max.format())
    )
}

fun handleReferendumCompleted(
    resourceManager: ResourceManager,
    failure: VoteValidationFailure.ReferendumCompleted
): TransformedFailure {
    return Default(
        resourceManager.getString(R.string.refrendum_vote_already_completed_title) to
            resourceManager.getString(R.string.refrendum_vote_already_completed_message)
    )
}
