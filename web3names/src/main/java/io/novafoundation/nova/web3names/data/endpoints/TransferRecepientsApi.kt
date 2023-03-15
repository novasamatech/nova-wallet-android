package io.novafoundation.nova.web3names.data.endpoints

import io.novafoundation.nova.web3names.data.endpoints.model.TransferRecipientRemote
import retrofit2.http.GET
import retrofit2.http.Url


interface TransferRecipientsApi {

    @GET
    suspend fun getTransferRecipients(
        @Url url: String
    ): Map<String, List<TransferRecipientRemote>>
}
