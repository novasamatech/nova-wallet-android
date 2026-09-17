package io.novafoundation.nova.infrastructure.attestation

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.Purpose
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Checked against the backend's own vectors: nova-infrastructure attestation/contracts/profile-2-vectors.json,
 * copied verbatim into test resources. Refresh the copy whenever the backend changes it.
 */
class AttestationSigningTest {

    private val fixture: JsonObject = JsonParser()
        .parse(javaClass.classLoader!!.getResource("profile-2-vectors.json")!!.readText())
        .asJsonObject

    private val vectors = fixture.getAsJsonArray("vectors").map { it.asJsonObject }

    @Test
    fun `registration bindings match the backend`() {
        val withBinding = vectors.filter { it.has("binding") }
        assertTrue(withBinding.isNotEmpty())

        withBinding.forEach { vector ->
            assertEquals(vector.name(), vector.str("registration_preimage_hex"), vector.registrationPreimage().hex())
        }
    }

    @Test
    fun `every preimage matches the backend byte for byte`() {
        vectors.forEach { vector ->
            assertEquals(vector.name(), vector.str("body_sha256"), vector.bodyDigest().hex())
            assertEquals(vector.name(), vector.str("preimage_hex"), vector.preimage().hex())
        }
    }

    @Test
    fun `the digest and the Play Integrity request hash match the backend`() {
        vectors.forEach { vector ->
            val digest = vector.digest()

            assertEquals(vector.name(), vector.str("context_sha256"), digest.hex())
            assertEquals(vector.name(), vector.str("context_base64url"), AttestationSigning.requestHash(digest))
        }
    }

    @Test
    fun `the backend's signatures verify over our digest with SHA256withECDSA`() {
        // Proves both halves at once: we compute the same D, and signing D itself - not a hash of it -
        // with Android's SHA256withECDSA is what the backend verifies
        val publicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(fixture.str("android_public_key_base64").decodeBase64()!!.toByteArray()))

        val signed = vectors.filter { it.has("signature_base64") }
        assertTrue(signed.isNotEmpty())

        signed.forEach { vector ->
            val verified = Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(vector.digest())
                verify(vector.str("signature_base64").decodeBase64()!!.toByteArray())
            }

            assertTrue(vector.name(), verified)
        }
    }

    @Test
    fun `a public key encodes as the contract's key reference`() {
        val key = fixture.str("android_public_key_base64")

        assertEquals(key, AttestationSigning.base64(key.decodeBase64()!!.toByteArray()))
        // Base64 of a P-256 SubjectPublicKeyInfo is exactly the backend's key_reference limit
        assertEquals(124, key.length)
    }

    private fun JsonObject.str(name: String): String = get(name).asString

    private fun JsonObject.name() = str("name")

    private fun JsonObject.purpose() = Purpose.entries.first { it.wireName == str("purpose") }

    private fun JsonObject.context(): AttestationSigning.RequestContext = getAsJsonObject("context").let {
        AttestationSigning.RequestContext(
            method = it.str("method"),
            scheme = it.str("scheme"),
            authority = it.str("authority"),
            port = it.str("port"),
            path = it.str("path"),
            contentType = it.str("content_type")
        )
    }

    private fun JsonObject.registrationPreimage(): ByteArray = getAsJsonObject("binding").let {
        AttestationSigning.registrationPreimage(
            platform = it.str("platform"),
            appId = it.str("app_id"),
            attestationType = it.str("attestation_type"),
            keyReference = it.str("key_reference"),
            appAttestEnvironment = it.str("app_attest_environment")
        )
    }

    // A registration hashes its binding in place of the body
    private fun JsonObject.bodyDigest(): ByteArray {
        val signedBytes = if (has("binding")) registrationPreimage() else str("body_base64").decodeBase64()!!.toByteArray()

        return AttestationSigning.sha256(signedBytes)
    }

    private fun JsonObject.preimage(): ByteArray {
        val context = getAsJsonObject("context")

        return AttestationSigning.requestPreimage(purpose(), context.str("challenge"), context.str("client_id"), context(), bodyDigest())
    }

    private fun JsonObject.digest(): ByteArray = AttestationSigning.sha256(preimage())

    private fun ByteArray.hex(): String = toByteString().hex()
}
