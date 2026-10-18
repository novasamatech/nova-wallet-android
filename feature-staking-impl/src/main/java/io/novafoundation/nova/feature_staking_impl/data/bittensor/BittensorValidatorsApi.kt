package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface BittensorValidatorsApi {

    @GET
    suspend fun validators(@Url url: String): Response<BittensorValidatorsRemote>

    @GET
    suspend fun rootClaims(@Url url: String): Response<BittensorRootClaimsRemote>

    @GET
    suspend fun rootValidator(@Url url: String): Response<BittensorRootValidatorRemote>

    @GET
    suspend fun rankedValidators(@Url url: String): Response<BittensorRankedValidatorsRemote>

    @GET
    suspend fun recommendations(@Url url: String): Response<BittensorRecommendationsRemote>
}
