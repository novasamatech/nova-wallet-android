package io.novafoundation.nova.web3names.data.serviceEndpoint

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException

class W3NRecepient(
    val chainIdCaip19: String,
    val account: String,
    val description: String?
)

abstract class W3NServiceEndpointHandler(
    private val endpoint: ServiceEndpoint,
    private val transferRecipientApi: TransferRecipientsApi
) {

    suspend fun getRecipients(web3Name: String, chain: Chain): List<W3NRecepient> {
        val url = endpoint.urls.firstOrNull() ?: throw Web3NamesException.ValidAccountNotFoundException(web3Name, chain.id)
        val recipientsContent = transferRecipientApi.getTransferRecipientsRaw(url)

        if (!verifyIntegrity(serviceEndpointId = endpoint.id, serviceEndpointContent = recipientsContent)) {
            throw Web3NamesException.IntegrityCheckFailed(web3Name)
        }

        return parseRecipients(recipientsContent)
    }

    abstract fun verifyIntegrity(serviceEndpointId: String, serviceEndpointContent: String): Boolean

    protected abstract fun parseRecipients(content: String): List<W3NRecepient>
}
