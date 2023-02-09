package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.AllHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.ReferendumVotersRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateDelegatorsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateDetailedStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DelegateStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request.DirectHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.AllVotesResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegateDelegatorsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegateDetailedStatsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DelegateStatsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.DirectVotesResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response.ReferendumVotersResponse
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

    @POST
    suspend fun getDelegateDelegators(
        @Url url: String,
        @Body body: DelegateDelegatorsRequest
    ): SubQueryResponse<DelegateDelegatorsResponse>

    @POST
    suspend fun getAllHistoricalVotes(
        @Url url: String,
        @Body body: AllHistoricalVotesRequest
    ): SubQueryResponse<AllVotesResponse>

    @POST
    suspend fun getDirectHistoricalVotes(
        @Url url: String,
        @Body body: DirectHistoricalVotesRequest
    ): SubQueryResponse<DirectVotesResponse>

    @POST
    suspend fun getReferendumVoters(
        @Url url: String,
        @Body body: ReferendumVotersRequest
    ): SubQueryResponse<ReferendumVotersResponse>
}
