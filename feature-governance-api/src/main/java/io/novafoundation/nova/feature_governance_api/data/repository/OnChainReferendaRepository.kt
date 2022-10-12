package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface OnChainReferendaRepository {

    suspend fun undecidingTimeout(chainId: ChainId): BlockNumber

    suspend fun getTracks(chainId: ChainId): Collection<TrackInfo>

    suspend fun getOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum>

    suspend fun onChainReferendumFlow(chainId: ChainId, referendumId: ReferendumId): Flow<OnChainReferendum>

    suspend fun getReferendaExecutionBlocks(chainId: ChainId, approvedReferendaIds: Collection<ReferendumId>): Map<ReferendumId, BlockNumber>
}

suspend fun OnChainReferendaRepository.getTracksById(chainId: ChainId): Map<TrackId, TrackInfo> {
    return getTracks(chainId).associateBy(TrackInfo::id)
}
