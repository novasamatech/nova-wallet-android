package io.novafoundation.nova.feature_governance_impl.domain.referendum.voters

import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.voteType
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.flow.Flow

class RealReferendumVotersInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    @OnChainIdentity private val identityProvider: IdentityProvider,
    private val governanceSharedState: GovernanceSharedState,
) : ReferendumVotersInteractor {

    override fun votersFlow(referendumId: ReferendumId, type: VoteType): Flow<List<ReferendumVoter>> {
        return flowOf { votersOf(referendumId, type) }
    }

    private suspend fun votersOf(
        referendumId: ReferendumId,
        type: VoteType
    ): List<ReferendumVoter> {
        val selectedGovernanceOption = governanceSharedState.selectedOption()
        val chainAsset = selectedGovernanceOption.assetWithChain.asset

        val source = governanceSourceRegistry.sourceFor(selectedGovernanceOption)

        val voters = source.convictionVoting.votersOf(referendumId, chainAsset.chainId)
            .filter { it.vote.voteType() == type }
            .filter { it.vote is AccountVote.Standard }

        val votersAccountIds = voters.map { it.accountId }
        val identities = identityProvider.identitiesFor(votersAccountIds, chainAsset.chainId)

        return voters.map { voter ->
            ReferendumVoter(
                accountVote = voter.vote,
                accountId = voter.accountId,
                identity = identities[voter.accountId],
                chainAsset = chainAsset,
            )
        }.sortedByDescending { it.vote?.totalVotes.orZero() }
    }
}
