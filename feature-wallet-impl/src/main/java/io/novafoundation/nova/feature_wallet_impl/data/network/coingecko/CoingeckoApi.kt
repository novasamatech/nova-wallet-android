package io.novafoundation.nova.feature_wallet_impl.data.network.coingecko

import io.novafoundation.nova.common.data.network.coingecko.PriceInfo
import retrofit2.http.GET
import retrofit2.http.Query

interface CoingeckoApi {

    @GET("//api.coingecko.com/api/v3/simple/price")
    suspend fun getAssetPrice(
        @Query("ids") priceIds: String,
        @Query("vs_currencies") currency: String,
        @Query("include_24hr_change") includeRateChange: Boolean
    ): Map<String, PriceInfo>
}
