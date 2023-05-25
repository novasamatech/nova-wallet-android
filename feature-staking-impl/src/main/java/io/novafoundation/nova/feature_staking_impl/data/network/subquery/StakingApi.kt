package io.novafoundation.nova.feature_staking_impl.data.network.subquery

import io.novafoundation.nova.common.data.network.subquery.EraValidatorInfoQueryResponse
import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingEraValidatorInfosRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingPeriodRewardsRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingTotalRewardsRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.StakingPeriodRewardsResponse
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.StakingTotalRewardResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface StakingApi {

    @POST
    suspend fun getRewardsByPeriod(
        @Url url: String,
        @Body body: StakingPeriodRewardsRequest
    ): SubQueryResponse<StakingPeriodRewardsResponse>

    @POST
    suspend fun getTotalRewards(
        @Url url: String,
        @Body body: StakingTotalRewardsRequest
    ): SubQueryResponse<StakingTotalRewardResponse>

    @POST
    suspend fun getValidatorsInfo(
        @Url url: String,
        @Body body: StakingEraValidatorInfosRequest
    ): SubQueryResponse<EraValidatorInfoQueryResponse>
}
