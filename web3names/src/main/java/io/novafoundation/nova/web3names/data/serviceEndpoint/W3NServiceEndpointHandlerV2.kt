package io.novafoundation.nova.web3names.data.serviceEndpoint

import com.google.gson.Gson
import io.ipfs.multibase.Multibase
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.endpoints.model.TransferRecipientDetailsRemoteV2
import jp.co.soramitsu.fearless_utils.hash.Hasher.blake2b256
import org.erdtman.jcs.JsonCanonicalizer

private typealias RecipientsByChainV2 = Map<String, Map<String, TransferRecipientDetailsRemoteV2>>

class W3NServiceEndpointHandlerV2(
    endpoint: ServiceEndpoint,
    transferRecipientApi: TransferRecipientsApi,
    private val gson: Gson
) : W3NServiceEndpointHandler(endpoint, transferRecipientApi) {

    override fun verifyIntegrity(serviceEndpointId: String, serviceEndpointContent: String): Boolean = runCatching {
        val expectedHash = Multibase.decode(serviceEndpointId)
        val canonizedJson = JsonCanonicalizer(serviceEndpointContent).encodedString

        val actualHash = canonizedJson.encodeToByteArray().blake2b256()

        expectedHash.contentEquals(actualHash)
    }.getOrDefault(false)

    override fun parseRecipients(content: String): List<W3NRecepient> {
        val fromJson = gson.fromJson<RecipientsByChainV2>(content)
        return fromJson.flatMap { (chainId, recipients) ->
            recipients.map { recipient ->
                W3NRecepient(
                    chainIdCaip19 = chainId,
                    account = recipient.key,
                    description = recipient.value.description
                )
            }
        }
    }
}
