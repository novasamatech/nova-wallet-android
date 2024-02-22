package io.novafoundation.nova.web3names.data.serviceEndpoint

import com.google.gson.Gson
import io.ipfs.multibase.Multibase
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.endpoints.model.TransferRecipientDetailsRemoteV1
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256

private typealias RecipientsByChainV1 = Map<String, List<TransferRecipientDetailsRemoteV1>>

class W3NServiceEndpointHandlerV1(
    endpoint: ServiceEndpoint,
    transferRecipientApi: TransferRecipientsApi,
    private val gson: Gson
) : W3NServiceEndpointHandler(endpoint, transferRecipientApi) {

    override fun verifyIntegrity(serviceEndpointId: String, serviceEndpointContent: String): Boolean = runCatching {
        val expectedHash = Multibase.decode(serviceEndpointId)

        val actualHash = serviceEndpointContent.encodeToByteArray().blake2b256()

        expectedHash.contentEquals(actualHash)
    }.getOrDefault(false)

    override fun parseRecipients(content: String): List<W3NRecepient> {
        val recipients = gson.fromJson<RecipientsByChainV1>(content)
        return recipients.flatMap { (chainId, recipients) ->
            recipients.map { recipient ->
                W3NRecepient(
                    chainIdCaip19 = chainId,
                    account = recipient.account,
                    description = recipient.description
                )
            }
        }
    }
}
