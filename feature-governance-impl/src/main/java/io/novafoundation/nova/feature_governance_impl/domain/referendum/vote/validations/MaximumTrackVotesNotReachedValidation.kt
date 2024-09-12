package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.trackVotesNumber
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.state.selectedOption

class MaximumTrackVotesNotReachedValidation(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val governanceSharedState: GovernanceSharedState,
) : VoteReferendumValidation {

    override suspend fun validate(value: VoteReferendaValidationPayload): ValidationStatus<VoteReferendumValidationFailure> {
        val selectedGovernanceOption = governanceSharedState.selectedOption()
        val source = governanceSourceRegistry.sourceFor(selectedGovernanceOption)
        val chainId = selectedGovernanceOption.assetWithChain.chain.id

        val maxTrackVotes = source.convictionVoting.maxTrackVotes(chainId)
        val reachedMaxVotes = value.trackVoting.any { it.trackVotesNumber() >= maxTrackVotes.toInt() }

        return reachedMaxVotes isFalseOrError {
            VoteReferendumValidationFailure.MaxTrackVotesReached(maxTrackVotes)
        }
    }
}

fun VoteReferendumValidationSystemBuilder.maximumTrackVotesNotReached(
    governanceSourceRegistry: GovernanceSourceRegistry,
    governanceSharedState: GovernanceSharedState,
) {
    validate(MaximumTrackVotesNotReachedValidation(governanceSourceRegistry, governanceSharedState))
}
