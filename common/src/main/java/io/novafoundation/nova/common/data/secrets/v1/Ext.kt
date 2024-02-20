package io.novafoundation.nova.common.data.secrets.v1

import io.novasama.substrate_sdk_android.encrypt.keypair.BaseKeypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair

/**
 * Creates [Sr25519Keypair] if [nonce] is not null
 * Creates [BaseKeypair] otherwise
 */
fun Keypair(
    publicKey: ByteArray,
    privateKey: ByteArray,
    nonce: ByteArray? = null
) = if (nonce != null) {
    Sr25519Keypair(
        publicKey = publicKey,
        privateKey = privateKey,
        nonce = nonce
    )
} else {
    BaseKeypair(
        privateKey = privateKey,
        publicKey = publicKey
    )
}
