package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

// TODO gov1 polkassembly
class Gov1OffChainReferendaInfoRepository : OffChainReferendaInfoRepository {

    override suspend fun referendumPreviews(chain: Chain): List<OffChainReferendumPreview> {
        return emptyList()
    }

    override suspend fun referendumDetails(chain: Chain): OffChainReferendumDetails? {
        return null
    }
}
