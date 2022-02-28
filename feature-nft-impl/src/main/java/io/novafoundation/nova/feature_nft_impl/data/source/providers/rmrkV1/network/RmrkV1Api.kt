package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.network

import retrofit2.http.GET
import retrofit2.http.Path

interface RmrkV1Api {

    companion object {
        const val BASE_URL = "https://singular.rmrk.app/api/rmrk1/"
    }

    @GET("account/{address}")
    suspend fun getNfts(@Path("address") address: String): List<RmrkV1NftRemote>
}
