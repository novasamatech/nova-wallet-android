package io.novafoundation.nova.common.utils.ip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET

interface PublicIpReceiverApi {
    @GET("https://api.ipify.org/")
    suspend fun get(): String
}

class PublicIpReceiver(
    private val api: PublicIpReceiverApi
) : IpReceiver {
    override suspend fun get(): String = withContext(Dispatchers.IO) { api.get() }
}
