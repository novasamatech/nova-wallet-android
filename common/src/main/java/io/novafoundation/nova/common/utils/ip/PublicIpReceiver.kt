package io.novafoundation.nova.common.utils.ip

import retrofit2.http.GET

interface PublicIpReceiverApi {
    @GET("https://api.ipify.org/")
    suspend fun get(): String
}

class PublicIpReceiver(
    private val api: PublicIpReceiverApi
) : IpReceiver {
    override suspend fun get(): String {
        return api.get()
    }
}
