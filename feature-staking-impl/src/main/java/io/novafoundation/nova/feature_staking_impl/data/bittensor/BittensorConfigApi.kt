package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.TimeHeaderInterceptor
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface BittensorConfigApi {

    @GET
    @Headers(TimeHeaderInterceptor.LONG_CONNECT, TimeHeaderInterceptor.LONG_READ, TimeHeaderInterceptor.LONG_WRITE)
    suspend fun config(@Url url: String): BittensorConfigRemote
}
