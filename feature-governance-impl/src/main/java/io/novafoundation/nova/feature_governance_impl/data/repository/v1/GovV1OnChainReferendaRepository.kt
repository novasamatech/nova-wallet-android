package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class GovV1OnChainReferendaRepository(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : OnChainReferendaRepository {

    override suspend fun undecidingTimeout(chainId: ChainId): BlockNumber {
        // TODO"make nullable and return null"

        return Balance.ZERO
    }

    override suspend fun getTracks(chainId: ChainId): Collection<TrackInfo> {
        // TODO return single track

        return emptyList()
    }

    override suspend fun getTrackQueues(trackIds: Set<TrackId>, chainId: ChainId): Map<TrackId, TrackQueue> {
        // we do not support `in queue` status for gov v1 yet
        return emptyMap()
    }

    override suspend fun getAllOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum> {
        // TODO referenda fetching
        return emptyList()
    }

    override suspend fun getOnChainReferenda(chainId: ChainId, referendaIds: Collection<ReferendumId>): Map<ReferendumId, OnChainReferendum> {
        // TODO referenda fetching
        return emptyMap()
    }

    override suspend fun onChainReferendumFlow(chainId: ChainId, referendumId: ReferendumId): Flow<OnChainReferendum> {
        // TODO referenda fetching
        return emptyFlow()
    }

    override suspend fun getReferendaExecutionBlocks(
        chainId: ChainId,
        approvedReferendaIds: Collection<ReferendumId>
    ): Map<ReferendumId, BlockNumber> {
        // TODO approved statuses
        return emptyMap()
    }
}
