package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface OnChainReferendaRepository {

    suspend fun getTracks(chainId: ChainId): Collection<TrackInfo>

    suspend fun getOnChainReferenda(chainId: ChainId): Collection<OnChainReferendum>
}
