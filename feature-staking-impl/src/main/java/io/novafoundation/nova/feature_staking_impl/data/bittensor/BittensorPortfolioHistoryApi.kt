package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface BittensorPortfolioHistoryApi {

    @GET
    suspend fun history(
        @Url url: String,
        @Query("accountSubject") accountSubject: String,
        @Query("period") period: String
    ): BittensorPortfolioHistoryRemote
}
