package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateMetadata
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.delegation.OffChainDelegateStats

interface DelegationsRepository {

    suspend fun getOffChainDelegatesStats(recentVotesBlockThreshold: BlockNumber): List<OffChainDelegateStats>

    suspend fun getOffChainDelegatesMetadata(): List<OffChainDelegateMetadata>
}
