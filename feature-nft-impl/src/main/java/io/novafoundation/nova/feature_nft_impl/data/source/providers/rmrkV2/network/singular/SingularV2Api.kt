package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.singular

import io.novafoundation.nova.common.data.network.http.CacheControl
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url

interface SingularV2Api {

    companion object {
        const val BASE_URL = "https://singular.app/api/rmrk2/"
    }

    @GET("collection/{collectionId}")
    suspend fun getCollection(@Path("collectionId") collectionId: String): List<SingularV2CollectionRemote>

    @GET("account/{accountAddress}")
    @Headers(CacheControl.NO_CACHE)
    suspend fun getAccountNfts(@Path("accountAddress") accountAddress: String): List<SingularV2NftRemote>

    @GET
    suspend fun getIpfsMetadata(@Url url: String): SingularV2CollectionMetadata
}
