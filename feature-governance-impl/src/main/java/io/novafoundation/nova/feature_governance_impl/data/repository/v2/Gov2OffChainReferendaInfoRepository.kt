package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class Gov2OffChainReferendaInfoRepository : OffChainReferendaInfoRepository {

    override suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview> {
        // TODO there is no off chain source for governance 2.0. Waiting for Polkassembly team to create one
        return emptyList()
    }
}
