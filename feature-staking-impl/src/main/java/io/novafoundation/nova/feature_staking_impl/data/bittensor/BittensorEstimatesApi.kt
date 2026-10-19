package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface BittensorEstimatesApi {
    @GET
    suspend fun alphaYields(
        @Url url: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<BittensorAlphaYieldsRemote>

    @GET
    suspend fun operations(
        @Url url: String,
        @Query("accountSubject") address: String,
        @Query("page") page: Int
    ): Response<BittensorOperationsRemote>
}
