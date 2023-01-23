package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Delegation
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateDetailedStats
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.DelegateStats
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.metadata.DelegateMetadataApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.metadata.getDelegatesMetadata
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateDelegatorsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateDetailedStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegateDelegatorsResponse
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.accountIdOrNull
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceDelegations
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class Gov2DelegationsRepository(
    private val delegationsSubqueryApi: DelegationsSubqueryApi,
    private val delegateMetadataApi: DelegateMetadataApi,
) : DelegationsRepository {

    override suspend fun isDelegationSupported(): Boolean {
        return true
    }

    override suspend fun getDelegatesStats(
        recentVotesBlockThreshold: BlockNumber,
        chain: Chain
    ): List<DelegateStats> {
        val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return emptyList()
        val request = DelegateStatsRequest(recentVotesBlockThreshold)

        val response = delegationsSubqueryApi.getDelegateStats(externalApiLink, request)
        val delegateStats = response.data.delegates.nodes

        return delegateStats.map { delegate ->
            DelegateStats(
                accountId = chain.accountIdOf(delegate.address),
                delegationsCount = delegate.delegators,
                delegatedVotes = delegate.delegatorVotes,
                recentVotes = delegate.delegateVotes.totalCount
            )
        }
    }

    override suspend fun getDetailedDelegateStats(
        delegateAddress: String,
        recentVotesBlockThreshold: BlockNumber,
        chain: Chain
    ): DelegateDetailedStats? {
        val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return null
        val request = DelegateDetailedStatsRequest(delegateAddress, recentVotesBlockThreshold)

        val response = delegationsSubqueryApi.getDetailedDelegateStats(externalApiLink, request)
        val delegateStats = response.data.delegates.nodes.firstOrNull() ?: return null

        return DelegateDetailedStats(
            accountId = chain.accountIdOf(delegateAddress),
            delegationsCount = delegateStats.delegators,
            delegatedVotes = delegateStats.delegatorVotes,
            recentVotes = delegateStats.recentVotes.totalCount,
            allVotes = delegateStats.allVotes.totalCount
        )
    }

    override suspend fun getDelegatesMetadata(chain: Chain): List<DelegateMetadata> {
        return delegateMetadataApi.getDelegatesMetadata(chain).mapNotNull {
            val accountId = chain.accountIdOrNull(it.address) ?: return@mapNotNull null

            DelegateMetadata(
                accountId = accountId,
                shortDescription = it.shortDescription,
                longDescription = it.longDescription,
                profileImageUrl = it.image,
                isOrganization = it.isOrganization,
                name = it.name
            )
        }
    }

    override suspend fun getDelegateMetadata(chain: Chain, delegate: AccountId): DelegateMetadata? {
        return getDelegatesMetadata(chain)
            .find { it.accountId.contentEquals(delegate) }
    }

    override suspend fun getDelegationsTo(delegate: AccountId, chain: Chain): List<Delegation> {
        val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return emptyList()
        val delegateAddress = chain.addressOf(delegate)

        val request = DelegateDelegatorsRequest(delegateAddress)
        val response = delegationsSubqueryApi.getDelegateDelegators(externalApiLink, request)

        return response.data.delegations.nodes.map { mapDelegationFromRemote(it, chain, delegate) }
    }

    private fun mapDelegationFromRemote(
        delegation: DelegateDelegatorsResponse.DelegatorRemote,
        chain: Chain,
        delegate: AccountId
    ): Delegation {
        return Delegation(
            vote = Delegation.Vote(
                amount = delegation.delegation.amount,
                conviction = mapConvictionFromRemote(delegation.delegation.conviction)
            ),
            delegator = chain.accountIdOf(delegation.address),
            delegate = delegate
        )
    }

    private fun mapConvictionFromRemote(remote: String): Conviction {
        return Conviction.values().first { it.name == remote }
    }
}
