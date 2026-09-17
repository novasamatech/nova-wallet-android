package io.novafoundation.nova.infrastructure.attestation

import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Profile 2 byte framing, as fixed by the backend's client contract
 * (nova-infrastructure: attestation/docs/contracts/client-attestation-protocol.md, "Exact Profile 2 Bytes").
 *
 * Every field is a length-prefixed run of raw bytes - no separators, hex or JSON - so no value can
 * bleed into its neighbour. The backend's own vectors pin the output byte for byte, see AttestationSigningTest.
 */
object AttestationSigning {

    const val PROFILE = 2

    private val REQUEST_DOMAIN = "NOVA-ATTESTATION\u0000".toByteArray(Charsets.US_ASCII)
    private val REGISTRATION_DOMAIN = "NOVA-REGISTRATION\u0000".toByteArray(Charsets.US_ASCII)

    enum class Purpose(val wireName: String, internal val tag: Int) {
        REGISTER("register", 0x01),
        REQUEST("request", 0x02)
    }

    /**
     * The signed parts of an HTTP request. The query is deliberately absent: the backend does not sign
     * it, so nothing security-relevant may travel there.
     *
     * @param contentType the exact Content-Type header value sent, or empty when there is no body
     */
    class RequestContext(
        val method: String,
        val scheme: String,
        val authority: String,
        val port: String,
        val path: String,
        val contentType: String
    )

    /**
     * R - what a registration binds. It stands in for the body digest: the registration body carries its
     * own proof and token, so hashing it would be circular.
     */
    fun registrationPreimage(
        platform: String,
        appId: String,
        attestationType: String,
        keyReference: String,
        appAttestEnvironment: String
    ): ByteArray = ByteArrayOutputStream().apply {
        write(REGISTRATION_DOMAIN)
        write(u16(PROFILE))
        listOf(platform, appId, attestationType, keyReference, appAttestEnvironment).forEach { write(lp(it)) }
    }.toByteArray()

    fun requestPreimage(
        purpose: Purpose,
        challenge: String,
        clientId: String,
        context: RequestContext,
        bodyDigest: ByteArray
    ): ByteArray {
        require(bodyDigest.size == SHA256_BYTES) { "body digest must be $SHA256_BYTES bytes" }

        return ByteArrayOutputStream().apply {
            write(REQUEST_DOMAIN)
            write(u16(PROFILE))
            write(purpose.tag)
            listOf(
                challenge,
                clientId,
                context.method,
                context.scheme,
                context.authority,
                context.port,
                context.path,
                context.contentType
            ).forEach { write(lp(it)) }
            write(bodyDigest)
        }.toByteArray()
    }

    /** D - the 32 bytes that get signed, and that Play Integrity's requestHash is made from. */
    fun requestDigest(
        purpose: Purpose,
        challenge: String,
        clientId: String,
        context: RequestContext,
        bodyDigest: ByteArray
    ): ByteArray = sha256(requestPreimage(purpose, challenge, clientId, context, bodyDigest))

    fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    /** Google's requestHash: URL-safe Base64 of D, without padding. */
    fun requestHash(digest: ByteArray): String = digest.toByteString().base64Url().trimEnd('=')

    /** Standard padded Base64 - what the contract uses for keys and signatures. */
    fun base64(bytes: ByteArray): String = bytes.toByteString().base64()

    private fun u16(value: Int) = byteArrayOf((value shr 8).toByte(), value.toByte())

    private fun lp(value: String): ByteArray {
        val encoded = value.toByteArray(Charsets.UTF_8)
        return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(encoded.size).array() + encoded
    }
}

private const val SHA256_BYTES = 32
