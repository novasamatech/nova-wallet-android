package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

interface ReferendumSummaryApi {

    @GET
    suspend fun getReferendumSummary(
        @Url url: String,
        @Header("x-network") networkHeader: String?,
        @Header("x-ai-summary-api-key") summaryApiKey: String,
        @Query("postId") postId: Int,
        @Query("proposalType") proposalType: String = "referendums_v2"
    ): ReferendumSummaryResponse
}
