package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request.ReferendumSummariesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request.ReferendumSummaryRequest
import io.novafoundation.nova.runtime.ext.isSwapSupported
import io.novafoundation.nova.runtime.ext.summaryApiOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ReferendumSummaryDataSource {

    suspend fun loadSummary(chain: Chain, id: ReferendumId, languageCode: String): String?

    suspend fun loadSummaries(chain: Chain, ids: List<ReferendumId>, languageCode: String): Map<ReferendumId, String>?
}

class RealReferendumSummaryDataSource(
    val api: ReferendumSummaryApi
) : ReferendumSummaryDataSource {

    override suspend fun loadSummary(chain: Chain, id: ReferendumId, languageCode: String): String? {
        val summaryApi = chain.summaryApiOrNull() ?: return null

        val response = api.getReferendumSummary(
            summaryApi.url,
            ReferendumSummaryRequest(
                chainId = chain.id,
                languageIsoCode = languageCode,
                referendumId = id.value.toString()
            )
        )

        return response.summary
    }

    override suspend fun loadSummaries(chain: Chain, ids: List<ReferendumId>, languageCode: String): Map<ReferendumId, String>? {
        val summaryApi = chain.summaryApiOrNull() ?: return null

        val response = api.getReferendumSummaries(
            summaryApi.url,
            ReferendumSummariesRequest(
                chainId = chain.id,
                languageIsoCode = languageCode,
                referendumIds = ids.map { it.value.toString() }
            )
        )

        return response.associateBy { ReferendumId(it.referendumId.toBigInteger()) }
            .mapValues { it.value.summary }
    }
}
