package io.novafoundation.nova.feature_staking_impl.data.network.subquery

import io.novafoundation.nova.common.data.network.subquery.EraValidatorInfoQueryResponse
import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingEraValidatorInfosRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingSumRewardRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.SubQueryTotalRewardResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface StakingApi {

    @POST
    suspend fun getTotalReward(
        @Url url: String,
        @Body body: StakingSumRewardRequest
    ): SubQueryResponse<SubQueryTotalRewardResponse>

    @POST
    suspend fun getValidatorsInfo(
        @Url url: String,
        @Body body: StakingEraValidatorInfosRequest
    ): SubQueryResponse<EraValidatorInfoQueryResponse>
}
