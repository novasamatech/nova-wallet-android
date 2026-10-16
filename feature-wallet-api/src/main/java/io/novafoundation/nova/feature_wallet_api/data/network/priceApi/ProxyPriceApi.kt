package io.novafoundation.nova.feature_wallet_api.data.network.priceApi

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProxyPriceApi {

    @GET("/api/v3/coins/{id}/market_chart")
    suspend fun getLastCoinRange(
        @Path("id") id: String,
        @Query("vs_currency") currency: String,
        @Query("days") days: String
    ): CoinRangeResponse
}
