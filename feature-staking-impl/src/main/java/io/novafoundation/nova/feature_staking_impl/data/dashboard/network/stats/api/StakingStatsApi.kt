package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface StakingStatsApi {

    @POST("nova-wallet/subquery-staking")
    suspend fun fetchStakingStats(@Body request: StakingStatsRequest): SubQueryResponse<StakingStatsResponse>
}
