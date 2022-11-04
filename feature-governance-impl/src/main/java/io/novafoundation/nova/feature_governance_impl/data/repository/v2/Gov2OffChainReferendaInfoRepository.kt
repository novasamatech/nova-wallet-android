package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

// TODO there is no off chain source for governance 2.0. Waiting for Polkassembly team to create one
class Gov2OffChainReferendaInfoRepository : OffChainReferendaInfoRepository {

    override suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview> {
        return emptyList()
    }

    override suspend fun referendumDetails(referendumId: ReferendumId, chain: Chain): OffChainReferendumDetails? {
        return null
    }
}
