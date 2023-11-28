package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface OnChainReferendaRepository {

    suspend fun electorate(chainId: ChainId): Balance

    suspend fun undecidingTimeout(chainId: ChainId): BlockNumber

    suspend fun getTracks(chainId: ChainId): Collection<TrackInfo>

    suspend fun getTrackQueues(trackIds: Set<TrackId>, chainId: ChainId): Map<TrackId, TrackQueue>

    suspend fun getAllOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum>

    suspend fun getOnChainReferenda(chainId: ChainId, referendaIds: Collection<ReferendumId>): Map<ReferendumId, OnChainReferendum>

    suspend fun onChainReferendumFlow(chainId: ChainId, referendumId: ReferendumId): Flow<OnChainReferendum?>

    suspend fun getReferendaExecutionBlocks(chainId: ChainId, approvedReferendaIds: Collection<ReferendumId>): Map<ReferendumId, BlockNumber>
}

suspend fun OnChainReferendaRepository.getTracksById(chainId: ChainId): Map<TrackId, TrackInfo> {
    return getTracks(chainId).associateBy(TrackInfo::id)
}
