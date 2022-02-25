package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface RmrkV2Api {

    companion object {
        const val BASE_URL = "https://kanaria.rmrk.app/api/rmrk2/"
    }

    @GET("account-birds/{address}")
    suspend fun getBirds(@Path("address") address: String): List<RmrkV2NftRemote>

    @GET("account-items/{address}")
    suspend fun getItems(@Path("address") address: String): List<RmrkV2NftRemote>

    @GET
    suspend fun getIpfsMetadata(@Url url: String): RmrkV2NftMetadataRemote
}
