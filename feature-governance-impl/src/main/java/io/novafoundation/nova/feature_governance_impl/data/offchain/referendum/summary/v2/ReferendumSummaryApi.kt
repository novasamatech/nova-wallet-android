package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2

import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request.ReferendumSummariesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request.ReferendumSummaryRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.response.ReferendumSummaryResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ReferendumSummaryApi {

    @POST("/not-secure/api/v1/referendum-summaries/single")
    suspend fun getReferendumSummary(
        @Body body: ReferendumSummaryRequest
    ): ReferendumSummaryResponse

    @POST("/not-secure/api/v1/referendum-summaries/list")
    suspend fun getReferendumSummaries(
        @Body body: ReferendumSummariesRequest
    ): List<ReferendumSummaryResponse>
}
