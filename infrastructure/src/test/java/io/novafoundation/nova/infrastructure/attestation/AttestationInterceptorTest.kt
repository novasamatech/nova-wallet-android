package io.novafoundation.nova.infrastructure.attestation

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

private const val CLIENT_ID = "6f2c1e4a-0000-4000-8000-000000000001"

private val JSON = "application/json".toMediaType()

private class RecordingAttestationService : ClientAttestationService {

    val contexts = mutableListOf<AttestationSigning.RequestContext>()
    val bodies = mutableListOf<String>()
    val forgotten = mutableListOf<String>()

    override suspend fun proofHeaders(context: AttestationSigning.RequestContext, body: ByteArray): Map<String, String> {
        contexts += context
        bodies += String(body)

        return mapOf(
            HEADER_PROFILE to "2",
            HEADER_CLIENT_ID to CLIENT_ID,
            HEADER_CHALLENGE to "challenge-${contexts.size}",
            HEADER_SIGNATURE to "signature-${contexts.size}"
        )
    }

    override suspend fun forgetRegistration(clientId: String) {
        forgotten += clientId
    }
}

private class FailingAttestationService(private val failure: Exception) : ClientAttestationService {

    override suspend fun proofHeaders(context: AttestationSigning.RequestContext, body: ByteArray): Map<String, String> = throw failure

    override suspend fun forgetRegistration(clientId: String) = Unit
}

/** Stands in for the network: answers with the queued statuses and records what would have been sent. */
private class FakeBackend(vararg answers: Pair<Int, String>) : Interceptor {

    private val answers = ArrayDeque(answers.toList())

    val sent = mutableListOf<Request>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        sent += request

        val (code, body) = answers.removeFirst()

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body(body.toResponseBody(JSON))
            .build()
    }
}

private fun rejection(code: String) = 401 to """{"error":{"code":"$code","message":"test"}}"""

class AttestationInterceptorTest {

    // Nothing here reaches a network: signing fails first, or FakeBackend answers
    private val analyticsRequest = Request.Builder()
        .url("https://api.example.test/v1/analytics/events")
        .post("""{"v":1}""".toRequestBody("application/json; charset=UTF-8".toMediaType()))
        .build()

    private fun execute(service: ClientAttestationService, backend: FakeBackend, request: Request = analyticsRequest): Response {
        return OkHttpClient.Builder()
            .addInterceptor(AttestationInterceptor(service))
            .addInterceptor(backend)
            .build()
            .newCall(request)
            .execute()
    }

    private fun failureOf(failure: Exception): Throwable {
        return runCatching { execute(FailingAttestationService(failure), FakeBackend()) }
            .exceptionOrNull() ?: throw AssertionError("the call was expected to fail")
    }

    @Test
    fun `a signed request carries exactly what was signed`() {
        val service = RecordingAttestationService()
        val backend = FakeBackend(200 to "{}")

        execute(service, backend).close()

        val sent = backend.sent.single()
        assertEquals("2", sent.header(HEADER_PROFILE))
        assertEquals(CLIENT_ID, sent.header(HEADER_CLIENT_ID))
        assertEquals("challenge-1", sent.header(HEADER_CHALLENGE))
        assertEquals("signature-1", sent.header(HEADER_SIGNATURE))
        // The backend compares Content-Type exactly and accepts no uppercase charset
        assertEquals("application/json", sent.body!!.contentType().toString())
        assertEquals("""{"v":1}""", Buffer().apply(sent.body!!::writeTo).readUtf8())

        val context = service.contexts.single()
        assertEquals(
            listOf("POST", "https", "api.example.test", "443", "/v1/analytics/events", "application/json"),
            listOf(context.method, context.scheme, context.authority, context.port, context.path, context.contentType)
        )
        assertEquals("""{"v":1}""", service.bodies.single())
    }

    @Test
    fun `a non-default port is part of the signed authority`() {
        val service = RecordingAttestationService()
        val request = Request.Builder().url("https://api.example.test:8443/v1/bittensor/subnets").get().build()

        execute(service, FakeBackend(200 to "{}"), request).close()

        val context = service.contexts.single()
        assertEquals("api.example.test:8443", context.authority)
        assertEquals("8443", context.port)
        assertEquals("", context.contentType)
    }

    @Test
    fun `an expired challenge gets exactly one retry with a fresh proof`() {
        val service = RecordingAttestationService()
        val backend = FakeBackend(rejection("invalid_challenge"), 200 to "{}")

        val response = execute(service, backend)

        assertEquals(200, response.code)
        assertEquals(2, service.contexts.size)
        assertEquals("challenge-2", backend.sent.last().header(HEADER_CHALLENGE))
        assertTrue(service.forgotten.isEmpty())
    }

    @Test
    fun `an installation the backend forgot is registered again and retried once`() {
        val service = RecordingAttestationService()
        val backend = FakeBackend(rejection("unknown_client"), 200 to "{}")

        val response = execute(service, backend)

        assertEquals(200, response.code)
        assertEquals(listOf(CLIENT_ID), service.forgotten)
        assertEquals(2, service.contexts.size)
    }

    @Test
    fun `other rejections are not retried`() {
        val service = RecordingAttestationService()
        val backend = FakeBackend(rejection("invalid_proof"))

        val response = execute(service, backend)

        assertEquals(401, response.code)
        assertEquals(1, backend.sent.size)
    }

    @Test
    fun `an HTTP error while fetching a challenge leaves as an IOException`() {
        // Exactly the 11 Sep failure: the backend answered the challenge request with 415. As a raw
        // HttpException it escaped the interceptor and OkHttp rethrew it on its dispatcher thread.
        val httpError = HttpException(retrofit2.Response.error<Any>(415, "".toResponseBody()))

        val error = failureOf(httpError)

        assertTrue("expected an IOException, got $error", error is AttestationUnavailableException)
        assertSame(httpError, error.cause)
    }

    @Test
    fun `any other unexpected failure is contained the same way`() {
        val error = failureOf(IllegalStateException("keystore is unavailable"))

        assertTrue("expected an IOException, got $error", error is AttestationUnavailableException)
    }

    @Test
    fun `a rejection keeps its own type so it is still treated as permanent`() {
        val rejection = AttestationFailedException("Attestation rejected with HTTP 403")

        val error = failureOf(rejection)

        assertTrue("expected the rejection itself, got $error", error is IOException)
        assertSame(rejection, error)
    }
}
