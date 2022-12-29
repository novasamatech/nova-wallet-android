package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateStats
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.DelegationsSubqueryApi
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.request.DelegateStatsRequest
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.externalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi.GovernanceDelegations

class Gov2DelegationsRepository(
    private val delegationsSubqueryApi: DelegationsSubqueryApi
) : DelegationsRepository {

    override suspend fun getOffChainDelegatesStats(
        recentVotesBlockThreshold: BlockNumber,
        chain: Chain
    ): List<OffChainDelegateStats> {
        val externalApiLink = chain.externalApi<GovernanceDelegations>()?.url ?: return emptyList()
        val request = DelegateStatsRequest(recentVotesBlockThreshold)

        val response = delegationsSubqueryApi.getDelegateStats(externalApiLink, request)
        val delegateStats = response.data.delegates.nodes

        return delegateStats.map { delegate ->
            OffChainDelegateStats(
                accountId = chain.accountIdOf(delegate.address),
                delegationsCount = delegate.delegators,
                delegatedVotes = delegate.delegatorVotes,
                recentVotes = delegate.delegateVotes.totalCount
            )
        }
    }

    override suspend fun getOffChainDelegatesMetadata(): List<OffChainDelegateMetadata> {
        // TODO
        return emptyList()
    }
}
