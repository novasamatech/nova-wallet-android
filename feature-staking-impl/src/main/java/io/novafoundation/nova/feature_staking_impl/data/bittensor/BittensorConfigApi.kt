package io.novafoundation.nova.feature_staking_impl.data.bittensor

import retrofit2.http.GET
import retrofit2.http.Url

interface BittensorConfigApi {

    @GET
    suspend fun config(@Url url: String): BittensorConfigRemote
}
