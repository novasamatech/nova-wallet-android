package io.novafoundation.nova.feature_governance_api.data.source

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface GovernanceSource {

    val referenda: OnChainReferendaRepository

    val convictionVoting: ConvictionVotingRepository

    val offChainInfo: OffChainReferendaInfoRepository

    val dappsRepository: GovernanceDAppsRepository

    val preImageRepository: PreImageRepository

    val delegationsRepository: DelegationsRepository
}

fun ConvictionVotingRepository.trackLocksFlowOrEmpty(voterAccountId: AccountId?, chainAssetId: FullChainAssetId): Flow<Map<TrackId, Balance>> {
    return if (voterAccountId != null) {
        trackLocksFlow(voterAccountId, chainAssetId)
    } else {
        flowOf(emptyMap())
    }
}
