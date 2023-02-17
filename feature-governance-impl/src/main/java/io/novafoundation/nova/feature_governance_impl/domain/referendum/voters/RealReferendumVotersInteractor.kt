package io.novafoundation.nova.feature_governance_impl.domain.referendum.voters

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.getAllAccountIds
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.repository.getDelegatesMetadataOrEmpty
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.label.DelegateLabel
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.GenericVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVoterDelegator
import io.novafoundation.nova.feature_governance_api.domain.referendum.voters.ReferendumVotersInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.mapAccountTypeToDomain
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    ): List<ReferendumVoter> = coroutineScope {
        val selectedGovernanceOption = governanceSharedState.selectedOption()
        val chain = selectedGovernanceOption.assetWithChain.chain
        val chainAsset = selectedGovernanceOption.assetWithChain.asset

        val source = governanceSourceRegistry.sourceFor(selectedGovernanceOption)

        val metadatasDeferred = async {
            source.delegationsRepository.getDelegatesMetadataOrEmpty(chain)
                .associateBy { it.accountId.intoKey() }
        }

        val votersDeferred = async { source.convictionVoting.votersOf(referendumId, chain, type) }

        val metadatas = metadatasDeferred.await()
        val voters = votersDeferred.await()

        val votersAccountIds = voters.flatMap { it.getAllAccountIds() }

        val identities = identityProvider.identitiesFor(votersAccountIds, chainAsset.chainId)

        voters.map { voter ->
            ReferendumVoter(
                accountVote = voter.vote,
                voteType = type,
                accountId = voter.accountId,
                identity = identities[voter.accountId],
                chainAsset = chainAsset,
                metadata = metadatas[voter.accountId]?.let { mapDelegateMetadata(it) },
                delegators = voter.delegators.map { mapDelegator(it, metadatas, identities, chainAsset) }
            )
        }.sortedByDescending { it.vote.totalVotes }
    }

    private fun mapDelegator(
        delegation: Delegation,
        metadatas: Map<AccountIdKey, DelegateMetadata>,
        identities: Map<AccountIdKey, Identity?>,
        chainAsset: Chain.Asset
    ): ReferendumVoterDelegator {
        val delegatorId = delegation.delegator
        return ReferendumVoterDelegator(
            accountId = delegatorId,
            vote = GenericVoter.ConvictionVote(chainAsset.amountFromPlanks(delegation.vote.amount), delegation.vote.conviction),
            identity = identities[delegatorId],
            metadata = metadatas[delegatorId]?.let { mapDelegateMetadata(it) }
        )
    }

    private fun mapDelegateMetadata(metadata: DelegateMetadata): DelegateLabel.Metadata {
        return DelegateLabel.Metadata(
            accountType = mapAccountTypeToDomain(metadata.isOrganization),
            iconUrl = metadata.profileImageUrl,
            name = metadata.name
        )
    }
}
