package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.network

import retrofit2.http.GET
import retrofit2.http.Path

interface RmrkV1Api {

    @GET("https://singular.rmrk.app/api/rmrk1/account/{address}")
    suspend fun getNfts(@Path("address") address: String): List<RmrkV1nftRemote>
}
