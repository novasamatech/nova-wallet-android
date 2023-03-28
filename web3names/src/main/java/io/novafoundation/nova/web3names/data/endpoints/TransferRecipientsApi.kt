package io.novafoundation.nova.web3names.data.endpoints

import retrofit2.http.GET
import retrofit2.http.Url

interface TransferRecipientsApi {

    @GET
    suspend fun getTransferRecipientsRaw(
        @Url url: String
    ): String
}
