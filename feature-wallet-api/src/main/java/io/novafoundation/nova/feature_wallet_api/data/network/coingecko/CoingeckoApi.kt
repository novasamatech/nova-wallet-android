package io.novafoundation.nova.feature_wallet_api.data.network.coingecko

import retrofit2.http.GET
import retrofit2.http.Query

interface CoingeckoApi {

    companion object {
        fun getRecentRateFieldName(coingeckoId: String): String {
            return coingeckoId + "_24h_change"
        }
    }

    @GET("//api.coingecko.com/api/v3/simple/price")
    suspend fun getAssetPrice(
        @Query("ids") priceIds: String,
        @Query("vs_currencies") currency: String,
        @Query("include_24hr_change") includeRateChange: Boolean
    ): Map<String, Map<String, Double>>
}
