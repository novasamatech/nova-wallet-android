package io.novafoundation.nova.common.data.network

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
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

        class Status(val code: Int, val body: String = "", val afterRedirect: Boolean = false) : Answer

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

    /** Answers in place of the network: [mirror] for the mirror host, [gitHub] for every other host. */
    private class Backend(var mirror: Answer, var gitHub: Answer) : Interceptor {

        val attempts = mutableListOf<Attempt>()

        var onMirrorAttempt: () -> Unit = {}

        val urls get() = attempts.map { it.request.url.toString() }

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val attempt = Attempt(request, chain.connectTimeoutMillis(), chain.readTimeoutMillis())
            attempts += attempt
            val isMirror = request.url.host == MIRROR_HOST
            if (isMirror) onMirrorAttempt()

            return when (val answer = if (isMirror) mirror else gitHub) {
                is Answer.Failure -> throw answer.error
                Answer.CallCancelled -> {
                    chain.call().cancel()
                    throw IOException("Canceled")
                }
                is Answer.Status -> {
                    val body = TrackedBody(answer.body)
                    attempt.body = body
                    val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).message("test")
                    val redirect = if (answer.afterRedirect) response.code(302).build() else null

                    response.code(answer.code).body(body).priorResponse(redirect).build()
                }
            }
        }
    }

    private class RecordingLogger : Logger {

        val messages = mutableListOf<String>()

        override fun log(message: String?) {
            messages += message.orEmpty()
        }

        override fun log(throwable: Throwable?) = Unit
    }

    private val mirrorBaseUrl = MIRROR_BASE.toHttpUrl()

    private val backend = Backend(mirror = Answer.Status(200, "mirror"), gitHub = Answer.Status(200, "github"))

    private val logger = RecordingLogger()

    private var elapsedRealtimeMillis = 0L

    private val interceptor = NovaUtilsMirrorInterceptor(
        mirrorBaseUrl = mirrorBaseUrl,
        mirrorConnectTimeoutMillis = MIRROR_CONNECT_TIMEOUT_MILLIS,
        mirrorReadTimeoutMillis = MIRROR_READ_TIMEOUT_MILLIS,
        failureCoolDownMillis = COOL_DOWN_MILLIS,
        elapsedRealtimeMillis = { elapsedRealtimeMillis },
        logger = logger
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(STANDARD_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(STANDARD_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
        .addInterceptor(TimeHeaderInterceptor())
        .addInterceptor(interceptor)
        .addInterceptor(backend)
        .build()

    private fun execute(request: Request): Response = client.newCall(request).execute()

    private fun get(url: String) = Request.Builder().url(url).build()

    private fun read(url: String = CHAINS_URL): String = execute(get(url)).use { it.body!!.string() }

    private fun mirrored(gitHubUrl: String): String? = novaUtilsMirrorUrl(gitHubUrl.toHttpUrl(), mirrorBaseUrl)?.toString()

    @Test
    fun `nova-utils master files map to the mirror base plus the unchanged file path`() {
        val mapping = mapOf(
            CHAINS_URL to CHAINS_MIRROR_URL,
            "$GITHUB_REFS_MASTER/xcm/v8/transfers_dynamic.json" to "${MIRROR_BASE}xcm/v8/transfers_dynamic.json",
            "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg" to "${MIRROR_BASE}icons/tokens/white/v1/DOT.svg",
            "$GITHUB_MASTER/icons/My%20Token%231%3F.svg" to "${MIRROR_BASE}icons/My%20Token%231%3F.svg",
            "$GITHUB_MASTER/a%2Fb/c.json" to "${MIRROR_BASE}a%2Fb/c.json",
            "$CHAINS_URL?token=secret#fragment" to CHAINS_MIRROR_URL
        )
        val baseWithoutTrailingSlash = MIRROR_BASE.removeSuffix("/").toHttpUrl()

        mapping.forEach { (gitHubUrl, mirrorUrl) -> assertEquals(mirrorUrl, mirrored(gitHubUrl)) }
        assertEquals(CHAINS_MIRROR_URL, novaUtilsMirrorUrl(CHAINS_URL.toHttpUrl(), baseWithoutTrailingSlash).toString())
    }

    @Test
    fun `other urls have no mirror url`() {
        val notMirrored = OTHER_URLS + listOf(
            "https://raw.githubusercontent.com/polkadot-js/nova-utils/master/all.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/heads/develop/chains/v23/chains.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/tags/master/chains/v23/chains.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master/chains/",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master/chains//chains.json",
            "http://raw.githubusercontent.com/novasamatech/nova-utils/master/chains/v23/chains.json",
            "https://raw.githubusercontent.com:8443/novasamatech/nova-utils/master/chains/v23/chains.json",
            "https://github.com/novasamatech/nova-utils/master/chains/v23/chains.json",
            CHAINS_MIRROR_URL
        )

        notMirrored.forEach { assertNull(it, mirrored(it)) }
    }

    @Test
    fun `other urls and methods pass through with a single call`() {
        val post = Request.Builder().url(CHAINS_URL).post("payload".toRequestBody()).build()

        val bodies = OTHER_URLS.map(::read) + execute(post).use { it.body!!.string() }

        assertEquals(List(OTHER_URLS.size + 1) { "github" }, bodies)
        assertEquals(OTHER_URLS + CHAINS_URL, backend.urls)
    }

    @Test
    fun `mirror 200 and 304 are used and GitHub is not called`() {
        val request = Request.Builder().url(CHAINS_URL).header("If-None-Match", "\"etag\"").tag(String::class.java, "tag").build()

        val ok = execute(request).use { it.body!!.string() }
        backend.mirror = Answer.Status(304)
        val notModified = execute(request.newBuilder().head().build()).use { it.code }

        assertEquals("mirror", ok)
        assertEquals(304, notModified)
        assertEquals(listOf(CHAINS_MIRROR_URL, CHAINS_MIRROR_URL), backend.urls)
        assertEquals(listOf("GET", "HEAD"), backend.attempts.map { it.request.method })
        assertEquals(request.headers, backend.attempts.first().request.headers)
        assertEquals("tag", backend.attempts.first().request.tag(String::class.java))
        assertTrue(logger.messages.isEmpty())
    }

    @Test
    fun `any other mirror status falls back to the original GitHub request without a cool-down`() {
        val statuses = listOf(404, 503, 204, 206, 400, 403, 429, 500, 502, 504)

        statuses.forEach { status ->
            backend.mirror = Answer.Status(status, "mirror")
            backend.attempts.clear()

            val body = read()

            val (mirrorAttempt, gitHubAttempt) = backend.attempts
            assertEquals("github", body)
            assertEquals(CHAINS_MIRROR_URL, mirrorAttempt.request.url.toString())
            assertTrue("mirror $status response must be closed", mirrorAttempt.body!!.closed)
            assertEquals(CHAINS_URL, gitHubAttempt.request.url.toString())
        }
        assertEquals(statuses.map { "[NovaUtilsMirror] GitHub fallback for $CHAINS_GITHUB_PATH: status $it" }, logger.messages)
    }

    @Test
    fun `200 at the end of a redirect chain falls back to GitHub`() {
        backend.mirror = Answer.Status(200, "login page", afterRedirect = true)

        val bodies = listOf(read(), read())

        assertEquals(listOf("github", "github"), bodies)
        assertTrue(backend.attempts.first().body!!.closed)
        assertEquals(List(2) { "[NovaUtilsMirror] GitHub fallback for $CHAINS_GITHUB_PATH: redirect" }, logger.messages)
    }

    @Test
    fun `mirror network failure falls back to GitHub and skips the mirror until the cool-down ends`() {
        backend.mirror = Answer.Failure(IOException("connection refused"))
        val afterFailure = read()
        backend.mirror = Answer.Status(200, "mirror")

        elapsedRealtimeMillis += COOL_DOWN_MILLIS - 1
        val duringCoolDown = listOf(read(), read("$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg"))
        elapsedRealtimeMillis += 1
        val afterCoolDown = read()

        assertEquals(listOf("github", "github", "github", "mirror"), listOf(afterFailure) + duringCoolDown + afterCoolDown)
        assertEquals(2, backend.attempts.count { it.request.url.host == MIRROR_HOST })
        assertEquals(
            listOf(
                "[NovaUtilsMirror] GitHub fallback for $CHAINS_GITHUB_PATH: IOException",
                "[NovaUtilsMirror] Mirror skipped for $COOL_DOWN_MILLIS ms after IOException"
            ),
            logger.messages
        )
    }

    @Test
    fun `failure of a request that was in flight when the cool-down started does not announce it again`() {
        backend.mirror = Answer.Failure(IOException("connection refused"))
        // Another request fails and starts the cool-down while the first mirror attempt is still in flight
        backend.onMirrorAttempt = {
            backend.onMirrorAttempt = {}
            read("$GITHUB_MASTER/a.json")
        }

        read()

        assertEquals(1, logger.messages.count { "skipped" in it })
    }

    @Test
    fun `GitHub failure after a mirror failure carries the mirror failure`() {
        val mirrorFailure = IOException("mirror unreachable")
        val gitHubFailure = IOException("github unreachable")
        backend.mirror = Answer.Failure(mirrorFailure)
        backend.gitHub = Answer.Failure(gitHubFailure)

        val thrown = runCatching { read() }.exceptionOrNull()

        assertSame(gitHubFailure, thrown)
        assertEquals(listOf<Throwable>(mirrorFailure), gitHubFailure.suppressed.toList())
    }

    @Test
    fun `cancelled call is rethrown without a fallback or a cool-down`() {
        backend.mirror = Answer.CallCancelled

        val thrown = runCatching { read() }.exceptionOrNull()
        backend.mirror = Answer.Status(200, "mirror")
        val next = read()

        assertTrue(thrown is IOException)
        assertEquals(listOf(CHAINS_MIRROR_URL, CHAINS_MIRROR_URL), backend.urls)
        assertEquals("mirror", next)
        assertTrue(logger.messages.isEmpty())
    }

    @Test
    fun `mirror attempt timeouts are capped while the GitHub attempt keeps the request timeouts`() {
        val raisedTimeouts = Headers.Builder().add(TimeHeaderInterceptor.LONG_CONNECT).add(TimeHeaderInterceptor.LONG_READ).build()
        backend.mirror = Answer.Status(503)

        read()
        execute(Request.Builder().url("$GITHUB_MASTER/bittensor/v1/config.json").headers(raisedTimeouts).build()).close()

        val timeouts = backend.attempts.map { it.connectTimeoutMillis to it.readTimeoutMillis }
        val mirrorTimeouts = MIRROR_CONNECT_TIMEOUT_MILLIS to MIRROR_READ_TIMEOUT_MILLIS
        val standardTimeouts = STANDARD_TIMEOUT_MILLIS to STANDARD_TIMEOUT_MILLIS
        val longTimeouts = LONG_TIMEOUT_MILLIS to LONG_TIMEOUT_MILLIS
        assertEquals(listOf(mirrorTimeouts, standardTimeouts, mirrorTimeouts, longTimeouts), timeouts)
    }

    @Test
    fun `mirror attempt timeouts never exceed the request timeouts and replace an unlimited one`() {
        val shortTimeoutMillis = MIRROR_CONNECT_TIMEOUT_MILLIS / 2
        val quickClient = client.newBuilder()
            .connectTimeout(shortTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(UNLIMITED_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        quickClient.newCall(get(CHAINS_URL)).execute().close()

        assertEquals(shortTimeoutMillis, backend.attempts.single().connectTimeoutMillis)
        assertEquals(MIRROR_READ_TIMEOUT_MILLIS, backend.attempts.single().readTimeoutMillis)
    }

    private companion object {

        const val MIRROR_HOST = "mirror.test"

        const val MIRROR_BASE = "https://$MIRROR_HOST/v1/config/"

        const val GITHUB_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/master"

        const val GITHUB_REFS_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/heads/master"

        const val CHAINS_URL = "$GITHUB_MASTER/chains/v23/chains.json"

        const val CHAINS_GITHUB_PATH = "/novasamatech/nova-utils/master/chains/v23/chains.json"

        const val CHAINS_MIRROR_URL = "${MIRROR_BASE}chains/v23/chains.json"

        val OTHER_URLS = listOf(
            "https://raw.githubusercontent.com/novasamatech/nova-wallet-android-releases/master/updates/v1/entrypoint_dev.json",
            "https://raw.githubusercontent.com/novasamatech/opengov-delegate-registry/master/registry/polkadot.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/develop/chains/v23/chains.json",
            "https://tokens-price.test/api/v3/simple/price?ids=polkadot"
        )

        const val COOL_DOWN_MILLIS = 60_000L

        const val MIRROR_CONNECT_TIMEOUT_MILLIS = 3_000

        const val MIRROR_READ_TIMEOUT_MILLIS = 3_000

        const val STANDARD_TIMEOUT_MILLIS = 20_000

        const val LONG_TIMEOUT_MILLIS = 60_000

        const val UNLIMITED_TIMEOUT_MILLIS = 0L
    }
}
