package io.novafoundation.nova.feature_wallet_api.data.network.priceApi

import retrofit2.http.GET
import retrofit2.http.Query

interface CoingeckoApi {

    companion object {
        const val BASE_URL = "https://api.coingecko.com"

        fun getRecentRateFieldName(priceId: String): String {
            return priceId + "_24h_change"
        }
    }

    @GET("/api/v3/simple/price")
    suspend fun getAssetPrice(
        @Query("ids") priceIds: String,
        @Query("vs_currencies") currency: String,
        @Query("include_24hr_change") includeRateChange: Boolean
    ): Map<String, Map<String, Double?>>
}
