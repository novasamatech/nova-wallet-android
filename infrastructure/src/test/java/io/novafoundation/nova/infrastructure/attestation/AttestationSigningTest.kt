package io.novafoundation.nova.infrastructure.attestation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

private const val CHALLENGE = "TEST_CHALLENGE_abc123"
private const val CLIENT_ID = "6f2c1e4a-0000-4000-8000-000000000001"
private const val PUBLIC_KEY_B64 =
    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEexampleexampleexampleexampleexampleexampleexampleexampleexampleAA=="

private val BODY = """{"v":1,"platform":"android","app_version":"10.9.1"}""".toByteArray(Charsets.UTF_8)

class AttestationSigningTest {

    @Test
    fun `body digest matches backend vector`() {
        assertEquals(
            "2c3d64eac83fc3f8bc8fe383d202bf4cc4b5b3c88328c87cc6695f8ecb49f4e7",
            AttestationSigning.bodyDigestHex(BODY)
        )
    }

    @Test
    fun `empty body digest matches backend vector`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            AttestationSigning.bodyDigestHex(ByteArray(0))
        )
    }

    @Test
    fun `signing payload matches backend vector`() {
        assertEquals(
            "4469fb60ce2ad38af216bc5ac89188071392af288cfaa99eb62532843d9c5c92",
            AttestationSigning.signingPayload(CHALLENGE, CLIENT_ID, BODY).toHexString()
        )
    }

    @Test
    fun `shared secret token matches backend vector`() {
        val payload = AttestationSigning.attestationPayload(CHALLENGE, CLIENT_ID, PUBLIC_KEY_B64)

        assertEquals(
            "5d50e394f8b0abd605d0774569109bc2d5b01530b36f481e03ca6573c04040d2",
            AttestationSigning.sharedSecretToken("test-secret-0123456789abcdef", payload).toHexString()
        )
    }

    @Test
    fun `attestation payload matches backend vector`() {
        assertEquals(
            "8a3c4750a6533bfec06bf0aee6f0311facd3da7ce0788cdede02f249f060b70e",
            AttestationSigning.attestationPayload(CHALLENGE, CLIENT_ID, PUBLIC_KEY_B64).toHexString()
        )
    }

    @Test
    fun `different challenge produces different payload`() {
        assertNotEquals(
            AttestationSigning.signingPayload(CHALLENGE, CLIENT_ID, BODY).toHexString(),
            AttestationSigning.signingPayload("other", CLIENT_ID, BODY).toHexString()
        )
    }

    @Test
    fun `different body produces different payload`() {
        assertNotEquals(
            AttestationSigning.signingPayload(CHALLENGE, CLIENT_ID, BODY).toHexString(),
            AttestationSigning.signingPayload(CHALLENGE, CLIENT_ID, BODY + '!'.code.toByte()).toHexString()
        )
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}
