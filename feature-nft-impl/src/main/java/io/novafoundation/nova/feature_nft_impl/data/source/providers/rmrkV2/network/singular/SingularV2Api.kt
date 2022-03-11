package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.singular

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface SingularV2Api {

    companion object {
        const val BASE_URL = "https://singular.app/api/rmrk2/"
    }

    @GET("collection/{collectionId}")
    suspend fun getCollection(@Path("collectionId") collectionId: String): List<SingularV2CollectionRemote>

    @GET
    suspend fun getIpfsMetadata(@Url url: String): SingularV2CollectionMetadata
}
