package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface BittensorValidatorsApi {

    @GET
    suspend fun validators(@Url url: String): Response<BittensorValidatorsRemote>

    @GET
    suspend fun recommendations(@Url url: String): Response<BittensorRecommendationsRemote>
}
