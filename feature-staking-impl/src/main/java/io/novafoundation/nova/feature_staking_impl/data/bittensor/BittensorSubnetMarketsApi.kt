package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.http.GET
import retrofit2.http.Query

interface BittensorSubnetMarketsApi {

    @GET("/api/v3/coins/markets")
    suspend fun markets(
        @Query("category") category: String = "bittensor-subnets",
        @Query("vs_currency") currency: String = "usd",
        @Query("per_page") perPage: Int,
        @Query("page") page: Int
    ): List<BittensorSubnetMarketRemote>
}
