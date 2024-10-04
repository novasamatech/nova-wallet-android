package io.novafoundation.nova.feature_governance_impl.domain.referendum.details

import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryDataSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ReferendumDetailsRepository {

    suspend fun loadSummaries(chain: Chain, ids: List<ReferendumId>, selectedLanguage: Language): Map<ReferendumId, String>?
}

class RealReferendumDetailsRepository(
    private val referendumSummaryDataSource: ReferendumSummaryDataSource
) : ReferendumDetailsRepository {

    override suspend fun loadSummaries(chain: Chain, ids: List<ReferendumId>, selectedLanguage: Language): Map<ReferendumId, String>? {
        return referendumSummaryDataSource.loadSummaries(chain, ids, selectedLanguage.iso639Code)
    }
}
