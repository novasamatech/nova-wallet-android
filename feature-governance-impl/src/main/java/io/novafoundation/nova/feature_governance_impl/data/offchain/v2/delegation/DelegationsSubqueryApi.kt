package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.request.DelegateStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.response.DelegateStatsResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface DelegationsSubqueryApi {

    @POST
    suspend fun getDelegateStats(
        @Url url: String,
        @Body body: DelegateStatsRequest
    ): SubQueryResponse<DelegateStatsResponse>
}
