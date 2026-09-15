package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.Purpose
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.base64
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.requestDigest
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.sha256
import kotlinx.coroutines.runBlocking
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

private const val APP_PACKAGE = "io.novafoundation.nova.test"
private const val CHALLENGE = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8"
private const val INTEGRITY_TOKEN = "integrity-token"

private val INFRA_URL = "https://api.example.test/".toHttpUrl()
private val PUBLIC_KEY = "public-key".toByteArray()
private val BODY = """{"v":1}""".toByteArray()

private val REQUEST_CONTEXT = AttestationSigning.RequestContext(
    method = "POST",
    scheme = "https",
    authority = "api.example.test",
    port = "443",
    path = "/v1/analytics/events",
    contentType = "application/json"
)

private class RecordingKeyPairStore : AttestationKeyPairStore {

    val deleted = mutableListOf<String>()

    override fun publicKey(alias: String) = PUBLIC_KEY

    // Echoing the payload lets a test read back exactly which digest got signed
    override fun sign(alias: String, payload: ByteArray) = payload

    override fun delete(alias: String) {
        deleted += alias
    }
}

private class FakeAttestationApi : AttestationApi {

    val challengeRequests = mutableListOf<AttestationChallengeRequest>()
    val registrations = mutableListOf<AttestationRegisterRequest>()

    val challengeUrls = mutableListOf<String>()
    val registerUrls = mutableListOf<String>()

    val challengeFailures = ArrayDeque<HttpException>()
    val registerFailures = ArrayDeque<HttpException>()

    override suspend fun challenge(url: String, request: AttestationChallengeRequest): AttestationChallengeResponse {
        challengeUrls += url
        challengeRequests += request
        challengeFailures.removeFirstOrNull()?.let { throw it }

        return AttestationChallengeResponse(CHALLENGE)
    }

    override suspend fun register(url: String, request: AttestationRegisterRequest) {
        registerUrls += url
        registrations += request
        registerFailures.removeFirstOrNull()?.let { throw it }
    }
}

private fun httpError(status: Int, code: String) = HttpException(
    Response.error<Any>(status, """{"error":{"code":"$code","message":"test"}}""".toResponseBody("application/json".toMediaType()))
)

class ClientAttestationServiceTest {

    private lateinit var identity: RealAttestationIdentity
    private lateinit var keyPairStore: RecordingKeyPairStore
    private lateinit var api: FakeAttestationApi
    private lateinit var requestedHashes: MutableList<String>
    private var infraUrl: HttpUrl? = INFRA_URL

    @Before
    fun setUp() {
        identity = RealAttestationIdentity(InMemoryPreferences())
        keyPairStore = RecordingKeyPairStore()
        api = FakeAttestationApi()
        requestedHashes = mutableListOf()
        infraUrl = INFRA_URL
    }

    private fun service() = RealClientAttestationService(
        api = api,
        identity = identity,
        keyPairStore = keyPairStore,
        integrityTokens = IntegrityTokenSource { requestHash -> INTEGRITY_TOKEN.also { requestedHashes += requestHash } },
        appPackage = APP_PACKAGE,
        urls = InfrastructureUrls { infraUrl }
    )

    @Test
    fun `registration and the proof follow profile 2`() = runBlocking<Unit> {
        val headers = service().proofHeaders(REQUEST_CONTEXT, BODY)
        val clientId = identity.clientId()

        assertEquals(listOf("register", "request"), api.challengeRequests.map { it.purpose })
        assertTrue(api.challengeRequests.all { it.profile == 2 && it.client_id == clientId })

        val keyReference = base64(PUBLIC_KEY)
        val registerContext = AttestationSigning.RequestContext("POST", "https", "api.example.test", "443", "/v1/attestation/register", "application/json")
        val binding = AttestationSigning.registrationPreimage("android", APP_PACKAGE, "play_integrity", keyReference, "")
        val registerDigest = requestDigest(Purpose.REGISTER, CHALLENGE, clientId, registerContext, sha256(binding))

        val registration = api.registrations.single()
        assertEquals(2, registration.profile)
        assertEquals(clientId, registration.client_id)
        assertEquals(CHALLENGE, registration.challenge)
        assertEquals("android", registration.platform)
        assertEquals(APP_PACKAGE, registration.app_id)
        assertEquals("play_integrity", registration.attestation_type)
        assertEquals(keyReference, registration.key_reference)
        assertEquals("", registration.app_attest_environment)
        assertEquals(INTEGRITY_TOKEN, registration.integrity_token)
        // Google gets the digest as requestHash, and the key signs that same digest
        assertEquals(listOf(AttestationSigning.requestHash(registerDigest)), requestedHashes)
        assertEquals(base64(registerDigest), registration.signature)

        val proofDigest = requestDigest(Purpose.REQUEST, CHALLENGE, clientId, REQUEST_CONTEXT, sha256(BODY))
        assertEquals(
            mapOf(
                HEADER_PROFILE to "2",
                HEADER_CLIENT_ID to clientId,
                HEADER_CHALLENGE to CHALLENGE,
                HEADER_SIGNATURE to base64(proofDigest)
            ),
            headers
        )
        assertTrue(identity.isAttested())

        // Both calls go to the host the global config names
        assertEquals(listOf("https://api.example.test/v1/attestation/challenges"), api.challengeUrls.distinct())
        assertEquals(listOf("https://api.example.test/v1/attestation/register"), api.registerUrls)
    }

    @Test
    fun `without an infrastructure URL in the config nothing is sent`() = runBlocking<Unit> {
        infraUrl = null
        val service = service()

        assertFailsWith<InfrastructureUrlMissingException> { service.proofHeaders(REQUEST_CONTEXT, BODY) }
        assertTrue(api.challengeRequests.isEmpty())

        // Not a rejection: once the config names a host, the same service attests as usual
        infraUrl = INFRA_URL
        service.proofHeaders(REQUEST_CONTEXT, BODY)
        assertEquals(1, api.registrations.size)
    }

    @Test
    fun `a registered installation only spends a request challenge`() = runBlocking<Unit> {
        identity.markAttested()

        service().proofHeaders(REQUEST_CONTEXT, BODY)

        assertEquals(listOf("request"), api.challengeRequests.map { it.purpose })
        assertTrue(api.registrations.isEmpty())
    }

    @Test
    fun `a conclusive rejection stops further attempts`() = runBlocking<Unit> {
        api.registerFailures += httpError(401, "attestation_failed")
        val service = service()

        val error = assertFailsWith<AttestationFailedException> { service.proofHeaders(REQUEST_CONTEXT, BODY) }
        assertFailsWith<AttestationFailedException> { service.proofHeaders(REQUEST_CONTEXT, BODY) }

        // The code is what tells a missing allowlist entry from a failed device check
        assertTrue(error.message, error.message!!.contains("attestation_failed on register"))
        // Asking again would only burn the backend's daily Play Integrity budget
        assertEquals(1, api.registrations.size)
    }

    @Test
    fun `an expired registration challenge is simply retried next time`() = runBlocking<Unit> {
        api.registerFailures += httpError(401, "invalid_challenge")
        val service = service()

        assertFailsWith<AttestationUnavailableException> { service.proofHeaders(REQUEST_CONTEXT, BODY) }
        service.proofHeaders(REQUEST_CONTEXT, BODY)

        assertEquals(2, api.registrations.size)
        assertTrue(identity.isAttested())
    }

    @Test
    fun `a conflicting binding starts over as a new installation`() = runBlocking<Unit> {
        val original = identity.clientId()
        api.registerFailures += httpError(409, "client_already_registered")
        val service = service()

        assertFailsWith<AttestationUnavailableException> { service.proofHeaders(REQUEST_CONTEXT, BODY) }

        assertNotEquals(original, identity.clientId())
        assertEquals(listOf(original), keyPairStore.deleted)

        service.proofHeaders(REQUEST_CONTEXT, BODY)
        assertEquals(identity.clientId(), api.registrations.last().client_id)
    }

    @Test
    fun `a backend that forgot the installation gets the same identity registered again`() = runBlocking<Unit> {
        val original = identity.clientId()
        identity.markAttested()
        api.challengeFailures += httpError(401, "unknown_client")

        service().proofHeaders(REQUEST_CONTEXT, BODY)

        assertEquals(listOf("request", "register", "request"), api.challengeRequests.map { it.purpose })
        assertEquals(original, api.registrations.single().client_id)
        assertTrue(keyPairStore.deleted.isEmpty())
    }

    @Test
    fun `a refused challenge stops further attempts`() = runBlocking<Unit> {
        identity.markAttested()
        api.challengeFailures += httpError(403, "binding_not_allowed")
        val service = service()

        assertFailsWith<AttestationFailedException> { service.proofHeaders(REQUEST_CONTEXT, BODY) }
        assertFailsWith<AttestationFailedException> { service.proofHeaders(REQUEST_CONTEXT, BODY) }

        assertEquals(1, api.challengeRequests.size)
    }

    @Test
    fun `forgetting a registration keeps the identity and its key`() = runBlocking<Unit> {
        val current = identity.clientId()
        identity.markAttested()

        service().forgetRegistration(current)

        assertEquals(current, identity.clientId())
        assertFalse(identity.isAttested())
        assertTrue(keyPairStore.deleted.isEmpty())
    }

    @Test
    fun `forgetting an identity that already moved on changes nothing`() = runBlocking<Unit> {
        identity.clientId()
        identity.markAttested()

        service().forgetRegistration("11111111-2222-4333-8444-555555555555")

        assertTrue(identity.isAttested())
    }


    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit): T {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return error
            throw AssertionError("expected ${T::class.simpleName}, got $error", error)
        }
        throw AssertionError("expected ${T::class.simpleName}, nothing was thrown")
    }
}
