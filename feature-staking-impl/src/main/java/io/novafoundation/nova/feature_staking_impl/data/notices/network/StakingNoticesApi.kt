package io.novafoundation.nova.feature_staking_impl.data.notices.network

import retrofit2.http.GET
import retrofit2.http.Url

interface StakingNoticesApi {

    @GET
    suspend fun fetchStakingNotices(@Url url: String): String
}
