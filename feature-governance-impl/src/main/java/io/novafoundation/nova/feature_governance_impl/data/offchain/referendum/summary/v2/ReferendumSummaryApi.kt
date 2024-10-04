package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2

import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request.ReferendumSummariesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.request.ReferendumSummaryRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.response.ReferendumSummaryResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface ReferendumSummaryApi {

    @POST
    suspend fun getReferendumSummary(
        @Url url: String,
        @Body body: ReferendumSummaryRequest
    ): ReferendumSummaryResponse

    @POST
    suspend fun getReferendumSummaries(
        @Url url: String,
        @Body body: ReferendumSummariesRequest
    ): List<ReferendumSummaryResponse>
}
