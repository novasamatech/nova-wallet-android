package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.BuildConfig
import io.novafoundation.nova.runtime.ext.summaryApiOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ReferendumSummaryDataSource {

    suspend fun loadSummary(chain: Chain, id: ReferendumId, baseUrl: String): String
}

class RealReferendumSummaryDataSource(
    val api: ReferendumSummaryApi
) : ReferendumSummaryDataSource {

    override suspend fun loadSummary(chain: Chain, id: ReferendumId, baseUrl: String): String {
        val externalApi = chain.summaryApiOrNull()!!

        return api.getReferendumSummary(
            baseUrl,
            networkHeader = externalApi.network,
            summaryApiKey = BuildConfig.SUMMARY_API_KEY,
            postId = id.value.toInt()
        ).summary
    }
}
