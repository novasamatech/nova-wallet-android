package io.novafoundation.nova.feature_governance_impl.domain.referendum.details

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryDataSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ReferendumDetailsRepository {

    suspend fun loadSummary(chain: Chain, id: ReferendumId, baseUrl: String): String
}

class RealReferendumDetailsRepository(
    private val referendumSummaryDataSource: ReferendumSummaryDataSource
) : ReferendumDetailsRepository {

    override suspend fun loadSummary(chain: Chain, id: ReferendumId, baseUrl: String): String {
        return referendumSummaryDataSource.loadSummary(chain, id, baseUrl)
    }
}
