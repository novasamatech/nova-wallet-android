package io.novafoundation.nova.feature_governance_impl.domain.referendum.voters

import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.voteType
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class RealReferendumVotersInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    @OnChainIdentity private val identityProvider: IdentityProvider,
) : ReferendumVotersInteractor {

    override fun votersFlow(referendumId: ReferendumId, chain: Chain, type: VoteType): Flow<List<ReferendumVoter>> {
        return flowOf { votersOf(referendumId, chain, type) }
    }

    private suspend fun votersOf(
        referendumId: ReferendumId,
        chain: Chain,
        type: VoteType
    ): List<ReferendumVoter> {
        val source = governanceSourceRegistry.sourceFor(chain.id)

        val voters = source.convictionVoting.votersOf(referendumId, chain.id)
            .filter { it.vote.voteType() == type }

        val votersAccountIds = voters.map { it.accountId }
        val identities = identityProvider.identitiesFor(votersAccountIds, chain.id)

        return voters.map { voter ->
            ReferendumVoter(
                vote = voter.vote,
                accountId = voter.accountId,
                identity = identities[voter.accountId]
            )
        }
    }
}
