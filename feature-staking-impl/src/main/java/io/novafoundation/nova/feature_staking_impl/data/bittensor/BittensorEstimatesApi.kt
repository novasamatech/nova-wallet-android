package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface BittensorEstimatesApi {
    /** One unpaginated collection per subnet; the route rejects `page` / `pageSize` (ADR-0045). */
    @GET
    suspend fun alphaYields(@Url url: String): Response<BittensorAlphaYieldsRemote>

    @GET
    suspend fun operations(
        @Url url: String,
        @Query("accountSubject") address: String,
        @Query("page") page: Int
    ): Response<BittensorOperationsRemote>
}
