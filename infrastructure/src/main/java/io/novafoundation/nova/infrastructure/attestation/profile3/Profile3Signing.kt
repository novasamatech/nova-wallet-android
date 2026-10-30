package io.novafoundation.nova.infrastructure.attestation.profile3

import io.novafoundation.nova.infrastructure.attestation.AttestationSigning
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/** Profile-3 bootstrap framing. Business requests carry an opaque bearer, not a per-request proof. */
object Profile3Signing {

    const val PROFILE = 3

    private const val API_MAJOR = 1

    private val REGISTRATION_DOMAIN = "NOVA-REGISTRATION\u0000".toByteArray(Charsets.US_ASCII)

    private val ISSUANCE_DOMAIN = "NOVA-TOKEN-ISSUANCE\u0000".toByteArray(Charsets.US_ASCII)

    private val ATTESTATION_DOMAIN = "NOVA-ATTESTATION\u0000".toByteArray(Charsets.US_ASCII)

    enum class Purpose(val wireName: String, internal val tag: Int) {
        REGISTER("register", 0x01),
        TOKEN("token", 0x03)
    }

    fun registrationPreimage(
        platform: String,
        appId: String,
        attestationType: String,
        keyReference: String,
        appAttestEnvironment: String
    ): ByteArray = ByteArrayOutputStream().apply {
        write(REGISTRATION_DOMAIN)
        write(unsignedShort(PROFILE))
        listOf(platform, appId, attestationType, keyReference, appAttestEnvironment).forEach { write(lengthPrefixed(it)) }
    }.toByteArray()

    fun issuancePreimage(publicOrigin: String): ByteArray = ByteArrayOutputStream().apply {
        write(ISSUANCE_DOMAIN)
        write(unsignedShort(PROFILE))
        write(unsignedShort(API_MAJOR))
        write(lengthPrefixed(publicOrigin))
    }.toByteArray()

    fun requestPreimage(
        purpose: Purpose,
        challenge: String,
        clientId: String,
        context: AttestationSigning.RequestContext,
        bodyDigest: ByteArray
    ): ByteArray {
        require(bodyDigest.size == 32) { "Expected a SHA-256 body digest" }
        return ByteArrayOutputStream().apply {
            write(ATTESTATION_DOMAIN)
            write(unsignedShort(PROFILE))
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
            ).forEach { write(lengthPrefixed(it)) }
            write(bodyDigest)
        }.toByteArray()
    }

    fun requestDigest(
        purpose: Purpose,
        challenge: String,
        clientId: String,
        context: AttestationSigning.RequestContext,
        bodyDigest: ByteArray
    ): ByteArray = AttestationSigning.sha256(requestPreimage(purpose, challenge, clientId, context, bodyDigest))

    private fun unsignedShort(value: Int): ByteArray = byteArrayOf((value shr 8).toByte(), value.toByte())

    private fun lengthPrefixed(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array() + bytes
    }
}
