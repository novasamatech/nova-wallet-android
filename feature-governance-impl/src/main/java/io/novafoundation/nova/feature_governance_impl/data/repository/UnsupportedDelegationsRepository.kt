package io.novafoundation.nova.feature_governance_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateStats
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class UnsupportedDelegationsRepository : DelegationsRepository {

    override suspend fun getOffChainDelegatesStats(recentVotesBlockThreshold: BlockNumber, chain: Chain): List<OffChainDelegateStats> {
        return emptyList()
    }

    override suspend fun getOffChainDelegatesMetadata(): List<OffChainDelegateMetadata> {
        return emptyList()
    }
}
