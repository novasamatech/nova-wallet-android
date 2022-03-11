package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.kanaria

import io.novafoundation.nova.common.data.network.http.CacheControl
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url

interface KanariaApi {

    companion object {
        const val BASE_URL = "https://kanaria.rmrk.app/api/rmrk2/"
    }

    @GET("account-birds/{address}")
    @Headers(CacheControl.NO_CACHE)
    suspend fun getBirds(@Path("address") address: String): List<RmrkV2NftRemote>

    @GET("account-items/{address}")
    @Headers(CacheControl.NO_CACHE)
    suspend fun getItems(@Path("address") address: String): List<RmrkV2NftRemote>

    @GET
    suspend fun getIpfsMetadata(@Url url: String): RmrkV2NftMetadataRemote
}
