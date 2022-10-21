package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.trackVotesNumber
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry

class MaximumTrackVotesNotReachedValidation(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
): VoteReferendumValidation {

    override suspend fun validate(value: VoteReferendumValidationPayload): ValidationStatus<VoteReferendumValidationFailure> {
        val chainId = value.asset.token.configuration.chainId
        val source = governanceSourceRegistry.sourceFor(chainId)

        val maxTrackVotes = source.convictionVoting.maxTrackVotes(chainId)
        val trackVotesNumber = value.trackVoting?.trackVotesNumber() ?: 0

        val reachedMaxVotes = trackVotesNumber >= maxTrackVotes.toInt()

        return reachedMaxVotes isFalseOrError {
            VoteReferendumValidationFailure.MaxTrackVotesReached(maxTrackVotes)
        }
    }
}

fun VoteReferendumValidationSystemBuilder.maximumTrackVotesNotReached(governanceSourceRegistry: GovernanceSourceRegistry) {
    validate(MaximumTrackVotesNotReachedValidation(governanceSourceRegistry))
}
