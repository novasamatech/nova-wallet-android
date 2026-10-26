package io.novafoundation.nova.infrastructure.attestation.profile3

import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Profile3InterceptorTest {

    private class Answer(val status: Int, val body: String = "{}", val challenge: String? = null, val json: Boolean = true)

    private class TrackedBody(text: String, private val json: Boolean) : ResponseBody() {
        var closed = false

        private val bytes = Buffer().writeUtf8(text)

        private val source = object : ForwardingSource(bytes) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()

        override fun contentType() = (if (json) "application/json" else "text/html").toMediaType()
        override fun contentLength() = -1L
        override fun source(): BufferedSource = source
    }

    private class Backend(vararg answers: Answer) : Interceptor {
        private val answers = ArrayDeque(answers.toList())

        val sent = mutableListOf<Request>()

        val bodies = mutableListOf<TrackedBody>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            sent += request
            val answer = answers.removeFirst()
            val body = TrackedBody(answer.body, answer.json)
            bodies += body
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(answer.status)
                .message("test")
                .body(body)
                .apply { answer.challenge?.let { header("WWW-Authenticate", it) } }
                .build()
        }
    }

    private class Provider : Profile3AccessTokenProvider {
        val clock = TestTokenClock()

        val random = Profile3TokenRandom { 0.0 }

        val factory = Profile3TokenRecordFactory(clock, random)

        val scope = Profile3TokenScope("https://infra.test", "client", 1)

        val invalidated = mutableListOf<Profile3TokenRecord>()

        val responses = mutableListOf<Int>()

        var calls = 0

        var waitForever = false

        var failAtCall: Int? = null

        override suspend fun token(origin: HttpUrl): Profile3TokenRecord {
            calls += 1
            if (waitForever) awaitCancellation()
            if (calls == failAtCall) throw IOException("Bootstrap unavailable")
            return factory.create(scope, "opaque-$calls", 1, 301, clock.now, clock.boot)
        }

        override suspend fun invalidate(record: Profile3TokenRecord) {
            invalidated += record
        }

        override suspend fun recordResponse(record: Profile3TokenRecord, status: Int, code: String?, retryAfter: String?) {
            responses += status
        }
    }

    private val gson = Gson()

    private val request = Request.Builder().url("https://infra.test/v1/bittensor/subnets?page=1").build()

    private fun execute(provider: Provider, backend: Backend, request: Request = this.request, timeout: Long = 150_000): Response {
        val interceptor = Profile3Interceptor(provider, gson, timeout)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor(backend).build()
        return client.newCall(request).execute()
    }

    @Test
    fun `business request has one bearer and no legacy proof headers`() {
        val provider = Provider()
        val backend = Backend(Answer(200))
        val request = request.newBuilder()
            .addHeader("Authorization", "old")
            .addHeader("X-Attestation-Profile", "2")
            .addHeader("X-Client-Id", "old")
            .addHeader("X-Challenge", "old")
            .addHeader("X-Signature", "old")
            .addHeader("X-App-Attest-Assertion", "old")
            .build()
        execute(provider, backend, request).close()
        assertEquals(listOf("Bearer opaque-1"), backend.sent.single().headers.values("Authorization"))
        assertFalse(backend.sent.single().headers.names().any { it.startsWith("X-", ignoreCase = true) })
    }

    @Test
    fun `valid invalid_token signal retries once preserving method path query body and headers`() {
        val provider = Provider()
        val backend = Backend(Answer(401, INVALID, CHALLENGE), Answer(200))
        val request = request.newBuilder().header("X-Request-ID", "operation").post("payload".toRequestBody()).build()
        execute(provider, backend, request).close()
        assertEquals(2, backend.sent.size)
        assertEquals(1, provider.invalidated.size)
        assertTrue(backend.bodies.first().closed)
        assertEquals(listOf("Bearer opaque-1", "Bearer opaque-2"), backend.sent.map { it.header("Authorization") })
        backend.sent.forEach {
            assertEquals(request.url, it.url)
            assertEquals("POST", it.method)
            assertEquals("operation", it.header("X-Request-ID"))
            val buffer = Buffer()
            it.body!!.writeTo(buffer)
            assertEquals("payload", buffer.readUtf8())
        }
    }

    @Test
    fun `second rejection invalidates second token but never refreshes or sends a third request`() {
        val provider = Provider()
        val backend = Backend(Answer(401, INVALID, CHALLENGE), Answer(401, INVALID, CHALLENGE))
        execute(provider, backend).use { assertEquals(401, it.code) }
        assertEquals(2, provider.calls)
        assertEquals(2, provider.invalidated.size)
        assertEquals(2, backend.sent.size)
    }

    @Test
    fun `generic missing header malformed or nonJSON 401 never refreshes`() {
        val answers = listOf(
            Answer(401, INVALID),
            Answer(401, "{}", CHALLENGE),
            Answer(401, "{broken", CHALLENGE),
            Answer(401, "{error:{code:invalid_token}}", CHALLENGE),
            Answer(401, INVALID + " trailing", CHALLENGE),
            Answer(401, INVALID, CHALLENGE, json = false),
            Answer(401, "{\"error\":{\"code\":\"unknown_client\"}}", CHALLENGE)
        )
        answers.forEach { answer ->
            val provider = Provider()
            val backend = Backend(answer)
            execute(provider, backend).close()
            assertEquals(1, provider.calls)
            assertTrue(provider.invalidated.isEmpty())
        }
    }

    @Test
    fun `HEAD requires status and challenge only`() {
        val provider = Provider()
        val backend = Backend(Answer(401, "", CHALLENGE), Answer(200))
        execute(provider, backend, request.newBuilder().head().build()).close()
        assertEquals(2, provider.calls)
        assertEquals(1, provider.invalidated.size)
    }

    @Test
    fun `400 403 429 and 503 do not trigger bearer renewal`() {
        listOf(400, 403, 429, 503).forEach { status ->
            val provider = Provider()
            val backend = Backend(Answer(status, INVALID, CHALLENGE))
            execute(provider, backend).close()
            assertEquals(1, provider.calls)
            assertTrue(provider.invalidated.isEmpty())
            if (status == 429 || status == 503) assertEquals(listOf(status), provider.responses)
        }
    }

    @Test
    fun `failed renewal closes denied response without reusing rejected bearer`() {
        val provider = Provider().apply { failAtCall = 2 }
        val backend = Backend(Answer(401, INVALID, CHALLENGE))
        assertTrue(runCatching { execute(provider, backend) }.exceptionOrNull() is IOException)
        assertEquals(1, backend.sent.size)
        assertTrue(backend.bodies.single().closed)
    }

    @Test
    fun `one shot body is authenticated but never replayed`() {
        val provider = Provider()
        val backend = Backend(Answer(401, INVALID, CHALLENGE))
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun isOneShot() = true
            override fun writeTo(sink: okio.BufferedSink) { sink.writeUtf8("stream") }
        }
        execute(provider, backend, request.newBuilder().post(body).build()).close()
        assertEquals("Bearer opaque-1", backend.sent.single().header("Authorization"))
        assertEquals(1, provider.calls)
        assertEquals(1, provider.invalidated.size)
    }

    @Test
    fun `foreign origin never receives a bearer`() {
        val provider = Provider()
        val backend = Backend(Answer(200))
        val foreign = request.newBuilder().url("https://foreign.test/").build()
        assertTrue(runCatching { execute(provider, backend, foreign) }.exceptionOrNull() is IOException)
        assertTrue(backend.sent.isEmpty())
    }

    @Test
    fun `bootstrap timeout becomes an IOException rather than crashing okhttp dispatcher`() {
        val provider = Provider().apply { waitForever = true }
        val backend = Backend(Answer(200))
        assertTrue(runCatching { execute(provider, backend, timeout = 20) }.exceptionOrNull() is IOException)
        assertTrue(backend.sent.isEmpty())
    }

    private companion object {
        const val INVALID = "{\"error\":{\"code\":\"invalid_token\"}}"
        const val CHALLENGE = "Bearer error=\"invalid_token\""
    }
}
