package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.AllHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.ReferendumVotersRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateDelegatorsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateDetailedStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateStatsByAddressesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DelegateStatsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.DirectHistoricalVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.ReferendumSplitAbstainVotersRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.ReferendumVotesRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.AllVotesResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DelegateDelegatorsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DelegateDetailedStatsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DelegateStatsResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.DirectVotesResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.ReferendumSplitAbstainVotersResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.ReferendumVotersResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.response.ReferendumVotesResponse
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
    suspend fun getDelegateStats(
        @Url url: String,
        @Body body: DelegateStatsByAddressesRequest
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

    @POST
    suspend fun getReferendumVotes(
        @Url url: String,
        @Body body: ReferendumVotesRequest
    ): SubQueryResponse<ReferendumVotesResponse>

    @POST
    suspend fun getReferendumAbstainVoters(
        @Url url: String,
        @Body body: ReferendumSplitAbstainVotersRequest
    ): SubQueryResponse<ReferendumSplitAbstainVotersResponse>
}
