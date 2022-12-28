package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface OffChainReferendaInfoRepository {

    suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview>

    suspend fun referendumDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumDetails?
}
