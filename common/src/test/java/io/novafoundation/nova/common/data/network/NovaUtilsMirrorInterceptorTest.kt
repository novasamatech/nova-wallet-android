package io.novafoundation.nova.common.data.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaUtilsMirrorInterceptorTest {

    private sealed interface Answer {

        class Status(val code: Int, val body: String = "") : Answer

        class Failure(val error: IOException) : Answer

        object CallCancelled : Answer
    }

    private class TrackedBody(text: String) : ResponseBody() {

        var closed = false

        private val source = object : ForwardingSource(Buffer().writeUtf8(text)) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()

        override fun contentType() = null

        override fun contentLength() = -1L

        override fun source(): BufferedSource = source
    }

    private class Attempt(val request: Request, val connectTimeoutMillis: Int, val readTimeoutMillis: Int) {

        var body: TrackedBody? = null
    }

    private class Backend(private val mirror: Answer, private val origin: Answer) : Interceptor {

        val attempts = mutableListOf<Attempt>()

        val urls get() = attempts.map { it.request.url.toString() }

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val attempt = Attempt(request, chain.connectTimeoutMillis(), chain.readTimeoutMillis())
            attempts += attempt

            val answer = if (request.url.host == MIRROR_HOST) mirror else origin

            return when (answer) {
                is Answer.Failure -> throw answer.error
                Answer.CallCancelled -> {
                    chain.call().cancel()
                    throw IOException("Canceled")
                }
                is Answer.Status -> {
                    val body = TrackedBody(answer.body)
                    attempt.body = body
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(answer.code)
                        .message("test")
                        .body(body)
                        .build()
                }
            }
        }
    }

    private val mirrorBaseUrl = MIRROR_BASE.toHttpUrl()

    private val interceptor = NovaUtilsMirrorInterceptor(
        mirrorBaseUrl = mirrorBaseUrl,
        mirrorAttemptTimeoutMillis = MIRROR_ATTEMPT_TIMEOUT_MILLIS,
        standardTimeoutMillis = STANDARD_TIMEOUT_MILLIS
    )

    private fun execute(backend: Backend, request: Request): Response {
        val client = OkHttpClient.Builder()
            .connectTimeout(STANDARD_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(STANDARD_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .addInterceptor(TimeHeaderInterceptor())
            .addInterceptor(interceptor)
            .addInterceptor(backend)
            .build()

        return client.newCall(request).execute()
    }

    private fun get(url: String) = Request.Builder().url(url).build()

    private fun mirrored(gitHubUrl: String): String? = novaUtilsMirrorUrl(gitHubUrl.toHttpUrl(), mirrorBaseUrl)?.toString()

    @Test
    fun `mirror 200 is used and GitHub is not called`() {
        val backend = Backend(mirror = Answer.Status(200, "mirror"), origin = Answer.Status(200, "github"))

        val response = execute(backend, get("$GITHUB_MASTER/chains/v23/chains.json"))

        assertEquals(200, response.code)
        assertEquals("mirror", response.body!!.string())
        assertEquals(listOf("${MIRROR_BASE}chains/v23/chains.json"), backend.urls)
    }

    @Test
    fun `mirror 304 is returned as is`() {
        val backend = Backend(mirror = Answer.Status(304), origin = Answer.Status(200, "github"))

        val response = execute(backend, get("$GITHUB_MASTER/chains/v23/chains.json"))

        assertEquals(304, response.code)
        assertEquals(1, backend.attempts.size)
        response.close()
    }

    @Test
    fun `mirror 404 falls back to the original GitHub request and closes the mirror response`() {
        assertFallsBackToGitHub(mirrorStatus = 404)
    }

    @Test
    fun `mirror 503 falls back to the original GitHub request and closes the mirror response`() {
        assertFallsBackToGitHub(mirrorStatus = 503)
    }

    @Test
    fun `any other mirror status falls back to GitHub`() {
        listOf(204, 206, 400, 403, 408, 429, 500, 502).forEach { assertFallsBackToGitHub(mirrorStatus = it) }
    }

    @Test
    fun `mirror network failure falls back to the original GitHub request`() {
        val backend = Backend(mirror = Answer.Failure(IOException("timeout")), origin = Answer.Status(200, "github"))
        val request = get("$GITHUB_MASTER/chains/v23/chains.json")

        val response = execute(backend, request)

        assertEquals("github", response.body!!.string())
        assertEquals(2, backend.attempts.size)
        assertEquals(request.url, backend.attempts.last().request.url)
    }

    @Test
    fun `GitHub answer is final after a failed mirror attempt`() {
        val gitHubFailure = IOException("github unreachable")
        val unreachable = Backend(mirror = Answer.Failure(IOException("mirror unreachable")), origin = Answer.Failure(gitHubFailure))
        val missing = Backend(mirror = Answer.Status(404), origin = Answer.Status(404, "github"))

        val thrown = runCatching { execute(unreachable, get("$GITHUB_MASTER/a.json")) }.exceptionOrNull()
        val response = execute(missing, get("$GITHUB_MASTER/a.json"))

        assertSame(gitHubFailure, thrown)
        assertEquals(404, response.code)
        assertEquals("github", response.body!!.string())
    }

    @Test
    fun `cancelled call does not fall back`() {
        val backend = Backend(mirror = Answer.CallCancelled, origin = Answer.Status(200, "github"))

        val thrown = runCatching { execute(backend, get("$GITHUB_MASTER/a.json")) }.exceptionOrNull()

        assertTrue(thrown is IOException)
        assertEquals(1, backend.attempts.size)
    }

    @Test
    fun `refs heads master form maps to the same mirror path`() {
        val backend = Backend(mirror = Answer.Status(200, "mirror"), origin = Answer.Status(200, "github"))

        execute(backend, get("$GITHUB_REFS_MASTER/xcm/v8/transfers_dynamic.json")).close()

        assertEquals(listOf("${MIRROR_BASE}xcm/v8/transfers_dynamic.json"), backend.urls)
        assertEquals(mirrored("$GITHUB_MASTER/xcm/v8/transfers_dynamic.json"), mirrored("$GITHUB_REFS_MASTER/xcm/v8/transfers_dynamic.json"))
    }

    @Test
    fun `nested and percent-encoded paths are preserved`() {
        assertEquals(
            "https://mirror.test/v1/config/icons/tokens/white/v1/DOT.svg",
            mirrored("$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg")
        )
        assertEquals(
            "https://mirror.test/v1/config/icons/My%20Token%231%3F.svg",
            mirrored("$GITHUB_MASTER/icons/My%20Token%231%3F.svg")
        )
        assertEquals(
            "https://mirror.test/v1/config/a%2Fb/c.json",
            mirrored("$GITHUB_MASTER/a%2Fb/c.json")
        )
    }

    @Test
    fun `mirror url is the mirror base plus the file path only`() {
        val baseWithoutTrailingSlash = MIRROR_BASE.removeSuffix("/").toHttpUrl()

        assertEquals(
            "https://mirror.test/v1/config/chains/v23/chains.json",
            novaUtilsMirrorUrl("$GITHUB_MASTER/chains/v23/chains.json".toHttpUrl(), baseWithoutTrailingSlash)?.toString()
        )
        assertEquals(
            "https://mirror.test/v1/config/chains/v23/chains.json",
            mirrored("$GITHUB_MASTER/chains/v23/chains.json?token=secret#fragment")
        )
    }

    @Test
    fun `urls that are not nova-utils master files have no mirror url`() {
        val notMirrored = listOf(
            "https://raw.githubusercontent.com/novasamatech/nova-wallet-android-releases/master/updates/v1/entrypoint_dev.json",
            "https://raw.githubusercontent.com/novasamatech/opengov-delegate-registry/master/registry/polkadot.json",
            "https://raw.githubusercontent.com/novasamatech/branding/master/logos/Nova_Wallet_Star_Color.png",
            "https://raw.githubusercontent.com/polkadot-js/nova-utils/master/all.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/develop/chains/v23/chains.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/heads/develop/chains/v23/chains.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/tags/master/chains/v23/chains.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master/",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master/chains/",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master/chains//chains.json",
            "http://raw.githubusercontent.com/novasamatech/nova-utils/master/chains/v23/chains.json",
            "https://raw.githubusercontent.com:8443/novasamatech/nova-utils/master/chains/v23/chains.json",
            "https://github.com/novasamatech/nova-utils/master/chains/v23/chains.json",
            "https://example.com/novasamatech/nova-utils/master/chains/v23/chains.json",
            "${MIRROR_BASE}chains/v23/chains.json"
        )

        notMirrored.forEach { assertNull(it, mirrored(it)) }
    }

    @Test
    fun `other repositories branches and hosts pass through with a single call`() {
        val passedThrough = listOf(
            "https://raw.githubusercontent.com/novasamatech/nova-wallet-android-releases/master/updates/v1/entrypoint_dev.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/develop/chains/v23/chains.json",
            "https://tokens-price.test/api/v3/simple/price?ids=polkadot"
        )

        passedThrough.forEach { url ->
            val backend = Backend(mirror = Answer.Status(200, "mirror"), origin = Answer.Status(200, "origin"))

            val response = execute(backend, get(url))

            assertEquals("origin", response.body!!.string())
            assertEquals(listOf(url), backend.urls)
        }
    }

    @Test
    fun `post passes through with a single call`() {
        val backend = Backend(mirror = Answer.Status(200, "mirror"), origin = Answer.Status(200, "origin"))
        val url = "$GITHUB_MASTER/chains/v23/chains.json"
        val request = Request.Builder().url(url).post("payload".toRequestBody()).build()

        execute(backend, request).close()

        assertEquals(listOf(url), backend.urls)
        assertEquals("POST", backend.attempts.single().request.method)
    }

    @Test
    fun `head is read from the mirror`() {
        val backend = Backend(mirror = Answer.Status(200), origin = Answer.Status(200))
        val request = Request.Builder().url("$GITHUB_MASTER/chains/v23/chains.json").head().build()

        execute(backend, request).close()

        assertEquals(listOf("${MIRROR_BASE}chains/v23/chains.json"), backend.urls)
        assertEquals("HEAD", backend.attempts.single().request.method)
    }

    @Test
    fun `mirror request keeps the method headers and tag and adds nothing`() {
        val backend = Backend(mirror = Answer.Status(200), origin = Answer.Status(200))
        val request = Request.Builder()
            .url("$GITHUB_MASTER/chains/v23/chains.json")
            .header("If-None-Match", "\"etag\"")
            .tag(String::class.java, "invocation")
            .build()

        execute(backend, request).close()

        val mirrorRequest = backend.attempts.single().request
        assertEquals(request.headers, mirrorRequest.headers)
        assertEquals("GET", mirrorRequest.method)
        assertEquals("invocation", mirrorRequest.tag(String::class.java))
    }

    @Test
    fun `mirror attempt is bounded while the GitHub fallback keeps the request timeouts`() {
        val backend = Backend(mirror = Answer.Status(503), origin = Answer.Status(200))

        execute(backend, get("$GITHUB_MASTER/chains/v23/chains.json")).close()

        val (mirrorAttempt, gitHubAttempt) = backend.attempts
        assertEquals(MIRROR_ATTEMPT_TIMEOUT_MILLIS, mirrorAttempt.connectTimeoutMillis)
        assertEquals(MIRROR_ATTEMPT_TIMEOUT_MILLIS, mirrorAttempt.readTimeoutMillis)
        assertEquals(STANDARD_TIMEOUT_MILLIS, gitHubAttempt.connectTimeoutMillis)
        assertEquals(STANDARD_TIMEOUT_MILLIS, gitHubAttempt.readTimeoutMillis)
    }

    @Test
    fun `timeouts raised for a request are not shortened for the mirror attempt`() {
        val backend = Backend(mirror = Answer.Status(503), origin = Answer.Status(200))
        val longTimeoutHeaders = Headers.Builder()
            .add(TimeHeaderInterceptor.LONG_CONNECT)
            .add(TimeHeaderInterceptor.LONG_READ)
            .build()
        val request = Request.Builder().url("$GITHUB_MASTER/bittensor/v1/config.json").headers(longTimeoutHeaders).build()

        execute(backend, request).close()

        backend.attempts.forEach { attempt ->
            assertEquals(LONG_TIMEOUT_MILLIS, attempt.connectTimeoutMillis)
            assertEquals(LONG_TIMEOUT_MILLIS, attempt.readTimeoutMillis)
        }
        assertEquals(2, backend.attempts.size)
    }

    @Test
    fun `timeouts shorter than the mirror bound are not extended`() {
        val backend = Backend(mirror = Answer.Status(200), origin = Answer.Status(200))
        val shortTimeoutMillis = MIRROR_ATTEMPT_TIMEOUT_MILLIS / 2
        val client = OkHttpClient.Builder()
            .connectTimeout(shortTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(shortTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
            .addInterceptor(interceptor)
            .addInterceptor(backend)
            .build()

        client.newCall(get("$GITHUB_MASTER/chains/v23/chains.json")).execute().close()

        assertEquals(shortTimeoutMillis, backend.attempts.single().connectTimeoutMillis)
        assertEquals(shortTimeoutMillis, backend.attempts.single().readTimeoutMillis)
    }

    private fun assertFallsBackToGitHub(mirrorStatus: Int) {
        val backend = Backend(mirror = Answer.Status(mirrorStatus, "mirror"), origin = Answer.Status(200, "github"))
        val request = get("$GITHUB_MASTER/chains/v23/chains.json")

        val response = execute(backend, request)

        assertEquals("github", response.body!!.string())
        val (mirrorAttempt, gitHubAttempt) = backend.attempts
        assertEquals("${MIRROR_BASE}chains/v23/chains.json", mirrorAttempt.request.url.toString())
        assertTrue("mirror $mirrorStatus response must be closed", mirrorAttempt.body!!.closed)
        assertEquals(request.url, gitHubAttempt.request.url)
        assertEquals(request.headers, gitHubAttempt.request.headers)
    }

    private companion object {

        const val MIRROR_HOST = "mirror.test"

        const val MIRROR_BASE = "https://$MIRROR_HOST/v1/config/"

        const val GITHUB_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/master"

        const val GITHUB_REFS_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/heads/master"

        const val MIRROR_ATTEMPT_TIMEOUT_MILLIS = 10_000

        const val STANDARD_TIMEOUT_MILLIS = 20_000

        const val LONG_TIMEOUT_MILLIS = 60_000
    }
}
