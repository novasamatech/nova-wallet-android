package io.novafoundation.nova.web3names.data.integrity

import io.ipfs.multibase.Multibase
import jp.co.soramitsu.fearless_utils.hash.Hasher.blake2b256

interface Web3NamesIntegrityVerifier {

    fun verifyIntegrity(serviceEndpointId: String, serviceEndpointContent: String): Boolean
}

class RealWe3NamesIntegrityVerifier : Web3NamesIntegrityVerifier {

    override fun verifyIntegrity(serviceEndpointId: String, serviceEndpointContent: String): Boolean = runCatching {
        val multiBaseEncodedHash = extractServiceEndpointHash(serviceEndpointId)
        val expectedHash = Multibase.decode(multiBaseEncodedHash)

        val actualHash = serviceEndpointContent.encodeToByteArray().blake2b256()

        expectedHash.contentEquals(actualHash)
    }.getOrDefault(false)

    private fun extractServiceEndpointHash(endpointId: String): String {
        // serviceEndpoint supposed to be an uri with hash stored in the fragment
        val (_, hash) = endpointId.split("#", limit = 2)

        return hash
    }
}
