package io.novafoundation.nova.feature_nft_impl.data.source.providers.common.network

import retrofit2.http.GET
import retrofit2.http.Url

interface IpfsApi {

    @GET
    suspend fun getIpfsMetadata(@Url url: String): UniquesNftsMetadata
}
