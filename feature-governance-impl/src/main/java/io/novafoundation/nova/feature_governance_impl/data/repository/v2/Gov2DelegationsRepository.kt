package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateStats
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository

class Gov2DelegationsRepository : DelegationsRepository {

    override suspend fun getOffChainDelegatesStats(recentVotesBlockThreshold: BlockNumber): List<OffChainDelegateStats> {
        // TODO
        return emptyList()
    }

    override suspend fun getOffChainDelegatesMetadata(): List<OffChainDelegateMetadata> {
        // TODO
        return emptyList()
    }
}
