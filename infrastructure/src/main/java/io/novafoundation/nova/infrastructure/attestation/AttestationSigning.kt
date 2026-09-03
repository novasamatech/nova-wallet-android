package io.novafoundation.nova.infrastructure.attestation

import io.novasama.substrate_sdk_android.extensions.toHexString
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val HMAC_ALGORITHM = "HmacSHA256"

object AttestationSigning {

    fun bodyDigestHex(body: ByteArray): String = sha256(body).toHexString()

    fun sharedSecretToken(secret: String, attestationPayload: ByteArray): ByteArray {
        return Mac.getInstance(HMAC_ALGORITHM).run {
            init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
            doFinal(attestationPayload)
        }
    }

    fun signingPayload(challenge: String, clientId: String, body: ByteArray): ByteArray {
        return sha256("$challenge$clientId${bodyDigestHex(body)}".toByteArray(Charsets.UTF_8))
    }

    fun attestationPayload(challenge: String, clientId: String, publicKeyBase64: String): ByteArray {
        return sha256("$challenge$clientId$publicKeyBase64".toByteArray(Charsets.UTF_8))
    }

    private fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)
}
