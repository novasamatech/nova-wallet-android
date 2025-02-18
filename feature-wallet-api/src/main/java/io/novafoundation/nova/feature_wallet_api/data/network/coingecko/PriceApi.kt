package io.novafoundation.nova.feature_wallet_api.data.network.coingecko

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PriceApi {

    companion object {
        fun getRecentRateFieldName(priceId: String): String {
            return priceId + "_24h_change"
        }
    }

    @GET("//tokens-price.novasama-tech.org/api/v3/coins/{id}/market_chart/range")
    suspend fun getCoinRange(
        @Path("id") id: String,
        @Query("vs_currency") currency: String,
        @Query("from") fromTimestamp: Long,
        @Query("to") toTimestamp: Long
    ): CoinRangeResponse

    @GET("//tokens-price.novasama-tech.org/api/v3/coins/{id}/market_chart")
    suspend fun getLastCoinRange(
        @Path("id") id: String,
        @Query("vs_currency") currency: String,
        @Query("days") days: String
    ): CoinRangeResponse

    @GET("//tokens-price.novasama-tech.org/api/v3/simple/price")
    suspend fun getAssetPrice(
        @Query("ids") priceIds: String,
        @Query("vs_currencies") currency: String,
        @Query("include_24hr_change") includeRateChange: Boolean
    ): Map<String, Map<String, Double?>>
}
