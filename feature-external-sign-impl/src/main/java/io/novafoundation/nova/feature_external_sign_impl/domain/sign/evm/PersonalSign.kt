package io.novafoundation.nova.feature_external_sign_impl.domain.sign.evm

import org.web3j.crypto.Hash
import org.web3j.crypto.Sign

private const val MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n"

private fun getEthereumMessagePrefix(messageLength: Int): ByteArray {
    return (MESSAGE_PREFIX + messageLength.toString()).encodeToByteArray()
}

/**
 * Adopted from [Sign.getEthereumMessageHash] since the former method is package-private and cannot be directly accessed by the calling code
 */
fun ByteArray.asEthereumPersonalSignMessage(): ByteArray {
    val prefix = getEthereumMessagePrefix(size)

    val result = ByteArray(prefix.size + size)

    System.arraycopy(prefix, 0, result, 0, prefix.size)
    System.arraycopy(this, 0, result, prefix.size, size)

    return Hash.sha3(result)
}
