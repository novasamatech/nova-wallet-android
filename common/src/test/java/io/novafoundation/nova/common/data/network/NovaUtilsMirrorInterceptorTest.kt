package io.novafoundation.nova.common.data.network

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.Call
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
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaUtilsMirrorInterceptorTest {

    private sealed interface Answer {

        class Status(val code: Int, val body: String = "", val declaredLength: Long = UNKNOWN_LENGTH) : Answer

        /** A 200 that ends a redirect chain. */
        class Redirected(val body: String) : Answer

        class Failure(val error: IOException) : Answer

        /** A 200 whose body fails while it is being read. */
        class BrokenBody(val error: IOException) : Answer

        /** Never answers; ends only when its call is cancelled. */
        object Hanging : Answer

        /** A 200 whose body never arrives; ends only when its call is cancelled. */
        object StalledBody : Answer
    }

    private class TrackedBody(content: Source, private val declaredLength: Long) : ResponseBody() {

        @Volatile
        var closed = false

        private val source = object : ForwardingSource(content) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()

        override fun contentType() = null

        override fun contentLength() = declaredLength

        override fun source(): BufferedSource = source
    }

    private class Attempt(val request: Request, val connectTimeoutMillis: Int, val readTimeoutMillis: Int) {

        @Volatile
        var body: TrackedBody? = null
    }

    private class Backend(@Volatile var answer: Answer) : Interceptor {

        val attempts = CopyOnWriteArrayList<Attempt>()

        val callCancelled = CountDownLatch(1)

        @Volatile
        var onAttempt: () -> Unit = {}

        val urls get() = attempts.map { it.request.url.toString() }

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val attempt = Attempt(request, chain.connectTimeoutMillis(), chain.readTimeoutMillis())
            attempts += attempt
            onAttempt()

            return when (val answer = answer) {
                is Answer.Failure -> throw answer.error
                Answer.Hanging -> throw awaitCancellation(chain.call())
                is Answer.Status -> respond(attempt, answer.code, Buffer().writeUtf8(answer.body), answer.declaredLength)
                is Answer.Redirected -> {
                    val redirect = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(302).message("test").build()
                    respond(attempt, 200, Buffer().writeUtf8(answer.body)).newBuilder().priorResponse(redirect).build()
                }
                is Answer.BrokenBody -> respond(attempt, 200, failingSource { answer.error })
                Answer.StalledBody -> respond(attempt, 200, failingSource { awaitCancellation(chain.call()) })
            }
        }

        private fun respond(attempt: Attempt, code: Int, content: Source, declaredLength: Long = UNKNOWN_LENGTH): Response {
            val body = TrackedBody(content, declaredLength)
            attempt.body = body

            return Response.Builder()
                .request(attempt.request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(body)
                .build()
        }

        private fun failingSource(failure: () -> IOException) = object : Source {

            override fun read(sink: Buffer, byteCount: Long): Long = throw failure()

            override fun timeout() = Timeout.NONE

            override fun close() = Unit
        }

        // Stands in for a socket that stays silent until OkHttp closes it on cancellation
        private fun awaitCancellation(call: Call): IOException {
            while (!call.isCanceled()) Thread.sleep(CANCELLATION_POLL_MILLIS)
            callCancelled.countDown()

            return IOException("Canceled")
        }
    }

    private class RecordingLogger : Logger {

        val messages = CopyOnWriteArrayList<String>()

        override fun log(message: String?) {
            messages += message.orEmpty()
        }

        override fun log(throwable: Throwable?) = Unit
    }

    private val mirrorBaseUrl = MIRROR_BASE.toHttpUrl()

    private val mirror = Backend(Answer.Status(200, "mirror"))

    private val gitHub = Backend(Answer.Status(200, "github"))

    private val logger = RecordingLogger()

    private val mirrorClient = OkHttpClient.Builder().addInterceptor(mirror).build()

    private var elapsedRealtimeMillis = 0L

    private var attemptDeadlineMillis = GENEROUS_DEADLINE_MILLIS

    private val client by lazy {
        val interceptor = NovaUtilsMirrorInterceptor(
            mirrorBaseUrl = mirrorBaseUrl,
            mirrorCallFactory = mirrorClient,
            attemptDeadlineMillis = attemptDeadlineMillis,
            failureCoolDownMillis = COOL_DOWN_MILLIS,
            maxBodySize = MAX_BODY_SIZE,
            elapsedRealtimeMillis = { elapsedRealtimeMillis },
            logger = logger
        )

        OkHttpClient.Builder()
            .connectTimeout(STANDARD_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(STANDARD_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .addInterceptor(TimeHeaderInterceptor())
            .addInterceptor(interceptor)
            .addInterceptor(gitHub)
            .build()
    }

    @After
    fun stopMirrorClient() {
        mirrorClient.dispatcher.executorService.shutdown()
    }

    private fun execute(request: Request): Response = client.newCall(request).execute()

    private fun get(url: String) = Request.Builder().url(url).build()

    private fun read(url: String): String = execute(get(url)).use { it.body!!.string() }

    private fun mirrored(gitHubUrl: String): String? = novaUtilsMirrorFile(gitHubUrl.toHttpUrl(), mirrorBaseUrl)?.url?.toString()

    @Test
    fun `mirror 200 is used and GitHub is not called`() {
        val response = execute(get(CHAINS_URL))

        assertEquals(200, response.code)
        assertEquals("mirror", response.body!!.string())
        assertEquals(listOf(CHAINS_MIRROR_URL), mirror.urls)
        assertTrue(gitHub.attempts.isEmpty())
        assertTrue(logger.messages.isEmpty())
    }

    @Test
    fun `mirror body is fully read before the answer is returned`() {
        val response = execute(get(CHAINS_URL))

        assertTrue(mirror.attempts.single().body!!.closed)
        assertEquals("mirror".length.toLong(), response.body!!.contentLength())
        assertEquals("mirror", response.body!!.string())
    }

    @Test
    fun `mirror 304 is returned as is`() {
        mirror.answer = Answer.Status(304)

        val response = execute(get(CHAINS_URL))

        assertEquals(304, response.code)
        assertEquals("", response.body!!.string())
        assertTrue(gitHub.attempts.isEmpty())
    }

    @Test
    fun `head is read from the mirror`() {
        mirror.answer = Answer.Status(200)
        val request = Request.Builder().url(CHAINS_URL).head().build()

        val response = execute(request)

        assertEquals(200, response.code)
        assertEquals("", response.body!!.string())
        assertEquals(listOf(CHAINS_MIRROR_URL), mirror.urls)
        assertEquals("HEAD", mirror.attempts.single().request.method)
        assertTrue(gitHub.attempts.isEmpty())
    }

    @Test
    fun `mirror 404 falls back to the original GitHub request and closes the mirror response`() {
        assertStatusFallsBackToGitHub(mirrorStatus = 404)
    }

    @Test
    fun `mirror 503 falls back to the original GitHub request and closes the mirror response`() {
        assertStatusFallsBackToGitHub(mirrorStatus = 503)
    }

    @Test
    fun `any other mirror status falls back to GitHub`() {
        listOf(204, 206, 301, 302, 400, 403, 408, 429, 500, 502, 504).forEach { assertStatusFallsBackToGitHub(mirrorStatus = it) }
    }

    @Test
    fun `mirror status does not start a cool-down`() {
        mirror.answer = Answer.Status(503)

        read(CHAINS_URL)
        read(CHAINS_URL)

        assertEquals(2, mirror.attempts.size)
        assertFalse(logger.messages.any { "skipped" in it })
    }

    @Test
    fun `200 at the end of a redirect chain falls back to GitHub`() {
        mirror.answer = Answer.Redirected("login page")

        val body = read(CHAINS_URL)

        assertEquals("github", body)
        assertTrue(mirror.attempts.single().body!!.closed)
        assertEquals(listOf("[NovaUtilsMirror] GitHub fallback for $CHAINS_PATH: redirect"), logger.messages)
    }

    @Test
    fun `mirror network failure falls back to the original GitHub request`() {
        mirror.answer = Answer.Failure(IOException("connection refused"))
        val request = get(CHAINS_URL)

        val response = execute(request)

        assertEquals("github", response.body!!.string())
        assertEquals(listOf(CHAINS_MIRROR_URL), mirror.urls)
        assertEquals(request.url, gitHub.attempts.single().request.url)
        assertEquals(
            listOf(
                "[NovaUtilsMirror] GitHub fallback for $CHAINS_PATH: IOException",
                "[NovaUtilsMirror] Mirror skipped for $COOL_DOWN_MILLIS ms after IOException"
            ),
            logger.messages
        )
    }

    @Test
    fun `mirror is skipped during the cool-down after a network failure and tried again after it`() {
        mirror.answer = Answer.Failure(IOException("connection refused"))
        read(CHAINS_URL)
        mirror.answer = Answer.Status(200, "mirror")

        elapsedRealtimeMillis += COOL_DOWN_MILLIS - 1
        val duringCoolDown = listOf(read(CHAINS_URL), read("$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg"))
        elapsedRealtimeMillis += 1
        val afterCoolDown = read(CHAINS_URL)

        assertEquals(listOf("github", "github"), duringCoolDown)
        assertEquals("mirror", afterCoolDown)
        assertEquals(2, mirror.attempts.size)
        assertEquals(3, gitHub.attempts.size)
        assertEquals(1, logger.messages.count { "skipped" in it })
    }

    @Test
    fun `failures during the cool-down of another request do not announce it again`() {
        mirror.answer = Answer.Failure(IOException("connection refused"))
        val firstCall = client.newCall(get(CHAINS_URL))
        val secondCall = client.newCall(get("$GITHUB_MASTER/a.json"))
        // The second request runs to its end while the first mirror attempt is still in flight
        mirror.onAttempt = {
            mirror.onAttempt = {}
            secondCall.execute().close()
        }

        firstCall.execute().close()

        assertEquals(2, mirror.attempts.size)
        assertEquals(1, logger.messages.count { "skipped" in it })
    }

    @Test
    fun `mirror body failure falls back to GitHub and starts the cool-down`() {
        mirror.answer = Answer.BrokenBody(IOException("unexpected end of stream"))

        val bodies = listOf(read(CHAINS_URL), read(CHAINS_URL))

        assertEquals(listOf("github", "github"), bodies)
        assertEquals(1, mirror.attempts.size)
        assertTrue(mirror.attempts.single().body!!.closed)
        assertEquals("[NovaUtilsMirror] GitHub fallback for $CHAINS_PATH: IOException", logger.messages.first())
    }

    @Test
    fun `body over the size limit falls back to GitHub without a cool-down`() {
        val tooLarge = "x".repeat(MAX_BODY_SIZE.toInt() + 1)
        mirror.answer = Answer.Status(200, tooLarge)
        val undeclared = read(CHAINS_URL)
        mirror.answer = Answer.Status(200, tooLarge, declaredLength = tooLarge.length.toLong())
        val declared = read(CHAINS_URL)

        assertEquals(listOf("github", "github"), listOf(undeclared, declared))
        assertEquals(2, mirror.attempts.size)
        assertTrue(mirror.attempts.all { it.body!!.closed })
        assertEquals(
            List(2) { "[NovaUtilsMirror] GitHub fallback for $CHAINS_PATH: body over $MAX_BODY_SIZE bytes" },
            logger.messages
        )
    }

    @Test
    fun `body of exactly the size limit is used`() {
        val largest = "x".repeat(MAX_BODY_SIZE.toInt())
        mirror.answer = Answer.Status(200, largest)

        assertEquals(largest, read(CHAINS_URL))
        assertTrue(gitHub.attempts.isEmpty())
    }

    @Test
    fun `mirror that does not answer within the deadline is cancelled and GitHub is read`() {
        attemptDeadlineMillis = SHORT_DEADLINE_MILLIS
        mirror.answer = Answer.Hanging

        val bodies = listOf(read(CHAINS_URL), read(CHAINS_URL))

        assertEquals(listOf("github", "github"), bodies)
        assertTrue(mirror.callCancelled.await(CANCELLATION_WAIT_MILLIS, TimeUnit.MILLISECONDS))
        assertEquals(1, mirror.attempts.size)
        assertEquals(
            listOf(
                "[NovaUtilsMirror] GitHub fallback for $CHAINS_PATH: SocketTimeoutException",
                "[NovaUtilsMirror] Mirror skipped for $COOL_DOWN_MILLIS ms after SocketTimeoutException"
            ),
            logger.messages
        )
    }

    @Test
    fun `the deadline covers the mirror body`() {
        attemptDeadlineMillis = SHORT_DEADLINE_MILLIS
        mirror.answer = Answer.StalledBody

        val body = read(CHAINS_URL)

        assertEquals("github", body)
        assertTrue(mirror.callCancelled.await(CANCELLATION_WAIT_MILLIS, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `timeouts raised for a request apply to the GitHub attempt only`() {
        attemptDeadlineMillis = SHORT_DEADLINE_MILLIS
        mirror.answer = Answer.Hanging
        val longTimeoutHeaders = Headers.Builder()
            .add(TimeHeaderInterceptor.LONG_CONNECT)
            .add(TimeHeaderInterceptor.LONG_READ)
            .build()
        val request = Request.Builder().url("$GITHUB_MASTER/bittensor/v1/config.json").headers(longTimeoutHeaders).build()
        val startedNanos = System.nanoTime()

        val body = execute(request).use { it.body!!.string() }

        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos)
        assertEquals("github", body)
        assertTrue("fell back after $elapsedMillis ms", elapsedMillis < LONG_TIMEOUT_MILLIS / 2)
        assertEquals(LONG_TIMEOUT_MILLIS, gitHub.attempts.single().connectTimeoutMillis)
        assertEquals(LONG_TIMEOUT_MILLIS, gitHub.attempts.single().readTimeoutMillis)
    }

    @Test
    fun `GitHub fallback keeps the request timeouts`() {
        mirror.answer = Answer.Status(503)

        read(CHAINS_URL)

        assertEquals(STANDARD_TIMEOUT_MILLIS, gitHub.attempts.single().connectTimeoutMillis)
        assertEquals(STANDARD_TIMEOUT_MILLIS, gitHub.attempts.single().readTimeoutMillis)
    }

    @Test
    fun `GitHub answer is final after a mirror status`() {
        mirror.answer = Answer.Status(404)
        gitHub.answer = Answer.Status(404, "github")

        val response = execute(get(CHAINS_URL))

        assertEquals(404, response.code)
        assertEquals("github", response.body!!.string())
    }

    @Test
    fun `GitHub failure after a mirror failure carries the mirror failure`() {
        val mirrorFailure = IOException("mirror unreachable")
        val gitHubFailure = IOException("github unreachable")
        mirror.answer = Answer.Failure(mirrorFailure)
        gitHub.answer = Answer.Failure(gitHubFailure)

        val thrown = runCatching { execute(get(CHAINS_URL)) }.exceptionOrNull()

        assertSame(gitHubFailure, thrown)
        assertEquals(listOf<Throwable>(mirrorFailure), gitHubFailure.suppressed.toList())
    }

    @Test
    fun `cancelling the call cancels the mirror attempt and does not fall back`() {
        mirror.answer = Answer.Hanging
        val call = client.newCall(get(CHAINS_URL))
        mirror.onAttempt = call::cancel

        val thrown = runCatching { call.execute() }.exceptionOrNull()
        mirror.answer = Answer.Status(200, "mirror")
        val next = read(CHAINS_URL)

        assertTrue(thrown is IOException)
        assertTrue(mirror.callCancelled.await(CANCELLATION_WAIT_MILLIS, TimeUnit.MILLISECONDS))
        assertTrue(gitHub.attempts.isEmpty())
        assertEquals("a cancelled call must not start the cool-down", "mirror", next)
    }

    @Test
    fun `refs heads master form maps to the same mirror path`() {
        read("$GITHUB_REFS_MASTER/xcm/v8/transfers_dynamic.json")

        assertEquals(listOf("${MIRROR_BASE}xcm/v8/transfers_dynamic.json"), mirror.urls)
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

        val mirrorFile = novaUtilsMirrorFile("$CHAINS_URL?token=secret#fragment".toHttpUrl(), baseWithoutTrailingSlash)

        assertEquals(CHAINS_MIRROR_URL, mirrorFile?.url?.toString())
        assertEquals(CHAINS_PATH, mirrorFile?.path)
        assertEquals(CHAINS_MIRROR_URL, mirrored("$CHAINS_URL?token=secret#fragment"))
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
            CHAINS_MIRROR_URL
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

        val bodies = passedThrough.map(::read)

        assertEquals(List(passedThrough.size) { "github" }, bodies)
        assertEquals(passedThrough, gitHub.urls)
        assertTrue(mirror.attempts.isEmpty())
    }

    @Test
    fun `post passes through with a single call`() {
        val request = Request.Builder().url(CHAINS_URL).post("payload".toRequestBody()).build()

        execute(request).close()

        assertEquals(listOf(CHAINS_URL), gitHub.urls)
        assertEquals("POST", gitHub.attempts.single().request.method)
        assertTrue(mirror.attempts.isEmpty())
    }

    @Test
    fun `mirror request keeps the method headers and tag and adds nothing`() {
        val request = Request.Builder()
            .url(CHAINS_URL)
            .header("If-None-Match", "\"etag\"")
            .tag(String::class.java, "invocation")
            .build()

        execute(request).close()

        val mirrorRequest = mirror.attempts.single().request
        assertEquals(request.headers, mirrorRequest.headers)
        assertEquals("GET", mirrorRequest.method)
        assertEquals("invocation", mirrorRequest.tag(String::class.java))
    }

    private fun assertStatusFallsBackToGitHub(mirrorStatus: Int) {
        mirror.answer = Answer.Status(mirrorStatus, "mirror")
        mirror.attempts.clear()
        gitHub.attempts.clear()
        logger.messages.clear()
        val request = get(CHAINS_URL)

        val response = execute(request)

        assertEquals("github", response.body!!.string())
        assertEquals(CHAINS_MIRROR_URL, mirror.attempts.single().request.url.toString())
        assertTrue("mirror $mirrorStatus response must be closed", mirror.attempts.single().body!!.closed)
        assertEquals(request.url, gitHub.attempts.single().request.url)
        assertEquals(request.headers, gitHub.attempts.single().request.headers)
        assertEquals(listOf("[NovaUtilsMirror] GitHub fallback for $CHAINS_PATH: status $mirrorStatus"), logger.messages)
    }

    private companion object {

        const val MIRROR_BASE = "https://mirror.test/v1/config/"

        const val GITHUB_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/master"

        const val GITHUB_REFS_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/heads/master"

        const val CHAINS_PATH = "chains/v23/chains.json"

        const val CHAINS_URL = "$GITHUB_MASTER/$CHAINS_PATH"

        const val CHAINS_MIRROR_URL = "$MIRROR_BASE$CHAINS_PATH"

        const val UNKNOWN_LENGTH = -1L

        const val MAX_BODY_SIZE = 64L

        const val COOL_DOWN_MILLIS = 60_000L

        // Never reached by a backend that answers at once
        const val GENEROUS_DEADLINE_MILLIS = 30_000L

        const val SHORT_DEADLINE_MILLIS = 200L

        const val CANCELLATION_POLL_MILLIS = 5L

        const val CANCELLATION_WAIT_MILLIS = 5_000L

        const val STANDARD_TIMEOUT_MILLIS = 20_000

        const val LONG_TIMEOUT_MILLIS = 60_000
    }
}
