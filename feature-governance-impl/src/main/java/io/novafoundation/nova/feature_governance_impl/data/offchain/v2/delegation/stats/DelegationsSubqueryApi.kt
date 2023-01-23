package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateDetailedStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegateDetailedStatsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegateStatsResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface DelegationsSubqueryApi {

    @POST
    suspend fun getDelegateStats(
        @Url url: String,
        @Body body: DelegateStatsRequest
    ): SubQueryResponse<DelegateStatsResponse>

    @POST
    suspend fun getDetailedDelegateStats(
        @Url url: String,
        @Body body: DelegateDetailedStatsRequest
    ): SubQueryResponse<DelegateDetailedStatsResponse>
}
