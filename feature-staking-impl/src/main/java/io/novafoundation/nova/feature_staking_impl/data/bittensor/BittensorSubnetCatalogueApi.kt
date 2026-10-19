package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface BittensorSubnetCatalogueApi {

    @GET
    suspend fun subnets(@Url url: String): Response<BittensorSubnetCatalogueRemote>
}
