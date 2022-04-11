package io.novafoundation.nova.feature_dapp_impl.data.network.metadata

import retrofit2.http.GET
import retrofit2.http.Url

interface DappMetadataApi {

    @GET
    suspend fun getParachainMetadata(@Url url: String): DappMetadataResponse
}
