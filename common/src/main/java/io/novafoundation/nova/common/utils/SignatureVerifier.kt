package io.novafoundation.nova.common.utils

import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper

interface SignatureVerifier {

    suspend fun verify(signature: SignatureWrapper, data: ByteArray, publicKey: ByteArray): Boolean
}

class RealSignatureVerifier : SignatureVerifier {

    override suspend fun verify(signature: SignatureWrapper, data: ByteArray, publicKey: ByteArray): Boolean {
        return when(signature) {
            is SignatureWrapper.Ecdsa -> {
                Sign
            }
            is SignatureWrapper.Sr25519 -> TODO()
            is SignatureWrapper.Ed25519 -> TODO()
        }
    }
}
