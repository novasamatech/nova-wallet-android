package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request.KodadotCollectionRequest
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request.KodadotMetadataRequest
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request.KodadotNftsRequest
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response.KodadotCollectionResponse
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response.KodadotMetadataResponse
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response.KodadotNftResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface KodadotApi {

    companion object {
        const val POLKADOT_ASSET_HUB_URL = "https://ahp.gql.api.kodadot.xyz"
        const val KUSAMA_ASSET_HUB_URL = "https://ahk.gql.api.kodadot.xyz"
    }

    @POST
    suspend fun getNfts(@Url url: String, @Body request: KodadotNftsRequest): SubQueryResponse<KodadotNftResponse>

    @POST
    suspend fun getCollection(@Url url: String, @Body request: KodadotCollectionRequest): SubQueryResponse<KodadotCollectionResponse>

    @POST
    suspend fun getMetadata(@Url url: String, @Body request: KodadotMetadataRequest): SubQueryResponse<KodadotMetadataResponse>
}
