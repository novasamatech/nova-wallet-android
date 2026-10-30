package io.novafoundation.nova.feature_staking_impl.data.bittensor.mev

import com.google.crypto.tink.aead.internal.InsecureNonceXChaCha20Poly1305
import io.novasama.substrate_sdk_android.hash.Hasher.xxHash128
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters
import java.security.SecureRandom

object BittensorMevProtocol {

    const val PUBLIC_KEY_BYTES = 1184

    const val KEM_CIPHERTEXT_BYTES = 1088

    const val NONCE_BYTES = 24

    const val OVERHEAD_BYTES = 1146

    const val MAX_PAYLOAD_BYTES = 8192

    const val ERA_BLOCKS = 8

    const val MAX_FINALITY_LAG = 5

    const val READ_TIMEOUT_BLOCKS = 2

    const val CONFIRMATION_WINDOW_BLOCKS = ERA_BLOCKS * 2 + 1

    const val SCAN_POLLS = (ERA_BLOCKS + MAX_FINALITY_LAG) * 2

    const val MIN_CONFIRMATION_TIMEOUT_MILLIS = 90_000L

    fun payloadSize(innerSize: Int): Int {
        require(innerSize >= 0 && innerSize <= Int.MAX_VALUE - OVERHEAD_BYTES)
        return innerSize + OVERHEAD_BYTES
    }
}

/** Randomness is injected so the complete wire format can be checked against the SDK vector. */
class BittensorMevSealer(private val secureRandom: SecureRandom) {

    fun seal(inner: ByteArray, publicKey: ByteArray): ByteArray {
        require(publicKey.size == BittensorMevProtocol.PUBLIC_KEY_BYTES)
        require(BittensorMevProtocol.payloadSize(inner.size) <= BittensorMevProtocol.MAX_PAYLOAD_BYTES)

        val key = MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, publicKey)
        val generator = MLKEMGenerator(secureRandom)
        val encapsulation = generator.generateEncapsulated(key)
        val sharedSecret = encapsulation.secret
        try {
            val kemCiphertext = encapsulation.encapsulation
            check(kemCiphertext.size == BittensorMevProtocol.KEM_CIPHERTEXT_BYTES)
            val nonce = ByteArray(BittensorMevProtocol.NONCE_BYTES).also(secureRandom::nextBytes)
            // The low-level Tink API accepts our fresh nonce; no nonce may be reused with this key.
            val cipher = InsecureNonceXChaCha20Poly1305(sharedSecret)
            val encrypted = cipher.encrypt(nonce, inner, byteArrayOf())
            val kemLength = byteArrayOf(
                BittensorMevProtocol.KEM_CIPHERTEXT_BYTES.toByte(),
                (BittensorMevProtocol.KEM_CIPHERTEXT_BYTES ushr 8).toByte()
            )
            return publicKey.xxHash128() + kemLength + kemCiphertext + nonce + encrypted
        } finally {
            sharedSecret.fill(0)
            encapsulation.destroy()
        }
    }
}
