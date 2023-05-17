package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import retrofit2.http.PUT

interface StakingStatsApi {

    @PUT
    suspend fun fetchStakingStats(request: StakingStatsRequest): SubQueryResponse<StakingStatsResponse>
}
