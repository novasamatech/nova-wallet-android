package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface StakingStatsApi {

    @POST
    suspend fun fetchStakingStats(
        @Body request: StakingStatsRequest,
        @Url url: String
    ): SubQueryResponse<StakingStatsResponse>
}
