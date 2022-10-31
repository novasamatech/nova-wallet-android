package io.novafoundation.nova.feature_governance_api.data.repository

import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface OffChainReferendaInfoRepository {

    suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview>

    suspend fun referendumDetails(chain: Chain): OffChainReferendumDetails?
}
