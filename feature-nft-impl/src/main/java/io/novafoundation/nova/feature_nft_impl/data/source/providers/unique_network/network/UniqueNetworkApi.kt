package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network

import io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response.UniqueNetworkCollection
import io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response.UniqueNetworkNft
import io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response.UniqueNetworkPaginatedResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

interface UniqueNetworkApi {
    companion object {
        const val BASE_URL = "https://api-unique.uniquescan.io/v2/"
    }

    @GET("nfts")
    suspend fun getNftsPage(
        @Query("ownerIn") owner: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("orderByTokenId") order: String = "asc"
    ): UniqueNetworkPaginatedResponse<UniqueNetworkNft>

    @GET("collections/{collectionId}")
    suspend fun getCollection(
        @Path("collectionId") collectionId: Int,
    ): UniqueNetworkCollection
}
