package io.novafoundation.nova.common.data.network

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.CacheControl
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The interceptor's decisions against a backend that answers in place of the network. What only the real OkHttp
 * engine can show (the deadline, the cache, redirects) is in [NovaUtilsMirrorRealEngineTest].
 */
class NovaUtilsMirrorInterceptorTest {

    private sealed interface Answer {

        /**
         * @param networkCode the status the server gave, or null for an answer of the local cache. Differs from
         * [code] when the cache has revalidated its copy: a 200 for the caller, a 304 on the network.
         * @param commit value of the `X-Config-Commit` header of a mirror answer, or null for an answer without it.
         */
        class Status(
            val code: Int,
            val body: String = "",
            val networkCode: Int? = code,
            val bodyFailure: IOException? = null,
            val commit: String? = COMMIT
        ) : Answer

        class Failure(val error: IOException) : Answer
    }

    private class FailingSource(private val failure: IOException) : Source {

        override fun read(sink: Buffer, byteCount: Long): Long = throw failure

        override fun timeout() = Timeout.NONE

        override fun close() = Unit
    }

    private class TrackedBody(content: Source) : ResponseBody() {

        var closed = false

        private val source = object : ForwardingSource(content) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()

        override fun contentType() = null

        override fun contentLength() = -1L

        override fun source(): BufferedSource = source
    }

    private class Attempt(
        val request: Request,
        val callTimeoutMillis: Long,
        val connectTimeoutMillis: Int,
        val readTimeoutMillis: Int
    ) {

        var body: TrackedBody? = null
    }

    /** Answers in place of the network: [mirror] for the mirror host, [gitHub] for every other host. */
    private class Backend(var mirror: Answer, var gitHub: Answer) : Interceptor {

        val attempts = mutableListOf<Attempt>()

        var onMirrorAttempt: () -> Unit = {}

        val urls get() = attempts.map { it.request.url.toString() }

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val callTimeoutMillis = TimeUnit.NANOSECONDS.toMillis(chain.call().timeout().timeoutNanos())
            val attempt = Attempt(request, callTimeoutMillis, chain.connectTimeoutMillis(), chain.readTimeoutMillis())
            attempts += attempt
            val isMirror = request.url.host == MIRROR_HOST
            if (isMirror) onMirrorAttempt()

            return when (val answer = if (isMirror) mirror else gitHub) {
                is Answer.Failure -> throw answer.error
                is Answer.Status -> {
                    val body = TrackedBody(answer.bodyFailure?.let(::FailingSource) ?: Buffer().writeUtf8(answer.body))
                    attempt.body = body
                    val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).message("test")
                    // GitHub knows no such header
                    if (isMirror && answer.commit != null) response.header(COMMIT_HEADER, answer.commit)
                    val networkResponse = answer.networkCode?.let { response.code(it).build() }

                    response.code(answer.code).body(body).networkResponse(networkResponse).build()
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

    private val backend = Backend(mirror = Answer.Status(200, MIRROR_BODY), gitHub = Answer.Status(200, GITHUB_BODY))

    private val logger = RecordingLogger()

    private var elapsedRealtimeMillis = 0L

    private val coolDown = NovaUtilsMirrorCoolDown(
        initialMillis = COOL_DOWN_MILLIS,
        maxMillis = MAX_COOL_DOWN_MILLIS,
        clock = { elapsedRealtimeMillis }
    )

    private val mirrorClient = OkHttpClient.Builder()
        .addInterceptor(backend)
        .build()

    private val interceptor = NovaUtilsMirrorInterceptor(
        mirrorBaseUrl = mirrorBaseUrl,
        mirrorClient = mirrorClient,
        maxAttemptMillis = MAX_ATTEMPT_MILLIS,
        maxBodyBytes = MAX_BODY_BYTES,
        coolDown = coolDown,
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

    private fun mirrorAttempts() = backend.attempts.count { it.request.url.host == MIRROR_HOST }

    private fun coolDownLogs() = logger.messages.filter { "skipped" in it }

    private fun fallbackLog(reason: String) = "[NovaUtilsMirror] GitHub fallback for $CHAINS_GITHUB_PATH: $reason"

    private fun coolDownLog(coolDownMillis: Long, reason: String) = "[NovaUtilsMirror] Mirror skipped for $coolDownMillis ms after $reason"

    private fun mirrorFails() {
        backend.mirror = Answer.Failure(IOException("connect timed out"))
    }

    private fun mirrorRecovers() {
        backend.mirror = Answer.Status(200, MIRROR_BODY)
        backend.onMirrorAttempt = {}
    }

    /** The mirror fails a request that GitHub answers, and the pause that starts passes. */
    private fun pauseMirrorAndWait() {
        mirrorFails()
        read()
        elapsedRealtimeMillis += COOL_DOWN_MILLIS
    }

    @Test
    fun `nova-utils master files map to the mirror base plus the unchanged file path`() {
        val mapping = mapOf(
            CHAINS_URL to CHAINS_MIRROR_URL,
            "$GITHUB_REFS_MASTER/xcm/v8/transfers_dynamic.json" to "${MIRROR_BASE}xcm/v8/transfers_dynamic.json",
            "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg" to "${MIRROR_BASE}icons/tokens/white/v1/DOT.svg",
            "$GITHUB_MASTER/icons/My%20Token%231%3F.svg" to "${MIRROR_BASE}icons/My%20Token%231%3F.svg",
            "$GITHUB_MASTER/a%2Fb/c.json" to "${MIRROR_BASE}a%2Fb/c.json",
            "$GITHUB_MASTER/icons/..hidden/.dot.file..svg" to "${MIRROR_BASE}icons/..hidden/.dot.file..svg",
            "$GITHUB_MASTER/icons/%252e%252e/a.svg" to "${MIRROR_BASE}icons/%252e%252e/a.svg",
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
    fun `path with a dot segment that appears after decoding has no mirror url`() {
        val notMirrored = listOf(
            "$GITHUB_MASTER/chains/..%2Fsecret.json",
            "$GITHUB_MASTER/chains/..%2fsecret.json",
            "$GITHUB_MASTER/chains/%2e%2e%2Fsecret.json",
            "$GITHUB_MASTER/chains/a%2F..%2Fsecret.json",
            "$GITHUB_MASTER/chains/a%2F%2E%2E",
            "$GITHUB_MASTER/chains/.%2Fchains.json",
            "$GITHUB_MASTER/chains/a%2F.%2Fchains.json",
            "$GITHUB_MASTER/chains/..%5Csecret.json",
            "$GITHUB_REFS_MASTER/chains/%2E%2E%5c..%2Fsecret.json"
        )

        notMirrored.forEach { assertNull(it, mirrored(it)) }
    }

    @Test
    fun `dot segments that OkHttp resolves while parsing never leave the nova-utils master tree through the mirror`() {
        // OkHttp has no URL with a dot segment: the GitHub request itself already names the resolved path
        val resolvedByOkHttp = mapOf(
            "$GITHUB_MASTER/chains/../chains/v23/chains.json" to CHAINS_MIRROR_URL,
            "$GITHUB_MASTER/chains/%2e%2e/chains/v23/chains.json" to CHAINS_MIRROR_URL,
            "$GITHUB_MASTER/chains/%2E./chains/./v23/%2e/chains.json" to CHAINS_MIRROR_URL,
            "$GITHUB_MASTER/../develop/chains/v23/chains.json" to null,
            "$GITHUB_MASTER/%2e%2e/develop/chains/v23/chains.json" to null,
            "$GITHUB_MASTER/chains/../../../../secret.json" to null,
            "$GITHUB_MASTER/chains/%2e%2e/%2E%2E/%2e./.%2E/secret.json" to null
        )

        resolvedByOkHttp.forEach { (gitHubUrl, mirrorUrl) -> assertEquals(gitHubUrl, mirrorUrl, mirrored(gitHubUrl)) }
    }

    @Test
    fun `other urls and methods pass through with a single call`() {
        val post = Request.Builder().url(CHAINS_URL).post("payload".toRequestBody()).build()
        val head = Request.Builder().url(CHAINS_URL).head().build()

        val bodies = OTHER_URLS.map(::read) + listOf(post, head).map { request -> execute(request).use { it.body!!.string() } }

        assertEquals(List(OTHER_URLS.size + 2) { GITHUB_BODY }, bodies)
        assertEquals(OTHER_URLS + CHAINS_URL + CHAINS_URL, backend.urls)
    }

    @Test
    fun `mirror 200 is used once its body has been read and GitHub is not called`() {
        val request = Request.Builder().url(CHAINS_URL).header("Accept", "application/json").tag(String::class.java, "tag").build()

        val response = execute(request)
        val bodyReadBeforeAnswer = backend.attempts.single().body!!.closed
        val body = response.use { it.body!!.string() }

        assertTrue(bodyReadBeforeAnswer)
        assertEquals(MIRROR_BODY, body)
        assertEquals(listOf(CHAINS_MIRROR_URL), backend.urls)
        assertEquals(request.headers, backend.attempts.single().request.headers)
        assertEquals("tag", backend.attempts.single().request.tag(String::class.java))
        assertTrue(logger.messages.isEmpty())
    }

    @Test
    fun `status that is not 200 falls back to the original GitHub request and the mirror is tried again next time`() {
        val statuses = listOf(404, 400, 401, 403, 410, 204, 206, 301, 302, 304)

        statuses.forEach { status ->
            backend.mirror = Answer.Status(status, MIRROR_BODY)
            backend.attempts.clear()

            val body = read()

            val (mirrorAttempt, gitHubAttempt) = backend.attempts
            assertEquals(GITHUB_BODY, body)
            assertEquals(CHAINS_MIRROR_URL, mirrorAttempt.request.url.toString())
            assertTrue("mirror $status response must be closed", mirrorAttempt.body!!.closed)
            assertEquals(CHAINS_URL, gitHubAttempt.request.url.toString())
        }
        assertEquals(statuses.map { fallbackLog("status $it") }, logger.messages)
    }

    @Test
    fun `server error and 429 fall back to GitHub and pause the mirror at once`() {
        val statuses = listOf(500, 502, 503, 504, 599, 429)
        // Not even GitHub has to answer: a status is the mirror's own word
        backend.gitHub = Answer.Failure(IOException("github unreachable"))

        statuses.forEach { status ->
            backend.mirror = Answer.Status(status, MIRROR_BODY)
            backend.attempts.clear()

            val failures = List(2) { runCatching { read() }.exceptionOrNull() }

            assertTrue(failures.all { it is IOException })
            assertEquals("mirror $status must be skipped by the second request", 1, mirrorAttempts())
            assertTrue(backend.attempts.first().body!!.closed)

            elapsedRealtimeMillis += COOL_DOWN_MILLIS
            mirrorRecovers()
            assertEquals(MIRROR_BODY, read())
        }
        assertEquals(statuses.map { coolDownLog(COOL_DOWN_MILLIS, "status $it") }, coolDownLogs())
    }

    @Test
    fun `200 with an unusable body falls back to GitHub without a pause`() {
        val jsonUrl = CHAINS_URL
        val upperCaseJsonUrl = "$GITHUB_MASTER/chains/v23/CHAINS.JSON"
        val iconUrl = "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg"
        val overLimit = "x".repeat(MAX_BODY_BYTES.toInt() + 1)
        val unusable = listOf(
            Triple(jsonUrl, "", "empty body"),
            Triple(iconUrl, "", "empty body"),
            Triple(jsonUrl, "<html>Sign in</html>", "body is not JSON"),
            Triple(jsonUrl, "[{\"chainId\":", "body is not JSON"),
            Triple(jsonUrl, "[] []", "body is not JSON"),
            Triple(upperCaseJsonUrl, "not json", "body is not JSON"),
            Triple(iconUrl, overLimit, "body over $MAX_BODY_BYTES bytes"),
            Triple(jsonUrl, "[\"${overLimit}\"]", "body over $MAX_BODY_BYTES bytes")
        )

        unusable.forEach { (url, mirrorBody, reason) ->
            backend.mirror = Answer.Status(200, mirrorBody)
            backend.attempts.clear()

            val body = read(url)

            assertEquals("$reason: $mirrorBody", GITHUB_BODY, body)
            assertEquals("the mirror must be tried every time", 1, mirrorAttempts())
            assertTrue(backend.attempts.first().body!!.closed)
            assertTrue(logger.messages.last(), logger.messages.last().endsWith(": $reason"))
        }
        assertTrue(coolDownLogs().isEmpty())
    }

    @Test
    fun `200 without a commit header is not the mirror's answer and does not pause the mirror`() {
        val iconUrl = "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg"
        val answers = listOf(
            Answer.Status(200, "<html>Blocked</html>", commit = null),
            Answer.Status(200, "<svg/>", commit = ""),
            Answer.Status(200, "<svg/>", commit = null, networkCode = null)
        )

        answers.forEach { answer ->
            backend.mirror = answer
            backend.attempts.clear()

            val body = read(iconUrl)

            assertEquals(GITHUB_BODY, body)
            assertEquals("the mirror must be tried every time", 1, mirrorAttempts())
            assertTrue(backend.attempts.first().body!!.closed)
            assertTrue(logger.messages.last(), logger.messages.last().endsWith(": no X-Config-Commit header"))
        }
        assertTrue(coolDownLogs().isEmpty())
    }

    @Test
    fun `200 with a usable body is the mirror's answer`() {
        val atLimit = "x".repeat(MAX_BODY_BYTES.toInt())
        val usable = listOf(
            CHAINS_URL to "{\"chains\": [1, 2.5, null, true, \"a\"]}\n",
            CHAINS_URL to "[]",
            "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg" to "<svg/>",
            "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg" to atLimit,
            "$GITHUB_MASTER/legal/v1/terms.md" to "not json"
        )

        val bodies = usable.map { (url, mirrorBody) ->
            backend.mirror = Answer.Status(200, mirrorBody)
            read(url)
        }

        assertEquals(usable.map { it.second }, bodies)
        assertEquals(usable.size, mirrorAttempts())
        assertTrue(logger.messages.isEmpty())
    }

    @Test
    fun `mirror failure pauses the mirror when GitHub answers the same request`() {
        mirrorFails()
        val afterFailure = read()
        mirrorRecovers()

        elapsedRealtimeMillis += COOL_DOWN_MILLIS - 1
        val duringCoolDown = listOf(read(), read("$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg"))
        elapsedRealtimeMillis += 1
        val afterCoolDown = read()

        assertEquals(listOf(GITHUB_BODY, GITHUB_BODY, GITHUB_BODY, MIRROR_BODY), listOf(afterFailure) + duringCoolDown + afterCoolDown)
        assertEquals(2, mirrorAttempts())
        assertEquals(listOf(fallbackLog("IOException"), coolDownLog(COOL_DOWN_MILLIS, "IOException")), logger.messages)
    }

    @Test
    fun `any status of GitHub over the network shows that the device is online`() {
        mirrorFails()
        backend.gitHub = Answer.Status(404)

        val statuses = List(2) { execute(get(CHAINS_URL)).use { it.code } }

        assertEquals(listOf(404, 404), statuses)
        assertEquals(1, mirrorAttempts())
    }

    @Test
    fun `mirror failure does not pause the mirror when GitHub fails as well`() {
        val mirrorFailure = IOException("mirror unreachable")
        val gitHubFailure = IOException("github unreachable")
        backend.mirror = Answer.Failure(mirrorFailure)
        backend.gitHub = Answer.Failure(gitHubFailure)

        val thrown = List(2) { runCatching { read() }.exceptionOrNull() }

        assertSame(gitHubFailure, thrown.first())
        assertTrue(mirrorFailure in gitHubFailure.suppressed)
        assertEquals("a device without connectivity must keep trying the mirror", 2, mirrorAttempts())
        assertTrue(coolDownLogs().isEmpty())
    }

    @Test
    fun `mirror failure does not pause the mirror when the GitHub answer comes from the cache`() {
        mirrorFails()
        backend.gitHub = Answer.Status(200, GITHUB_BODY, networkCode = null)

        val bodies = listOf(read(), read())

        assertEquals(listOf(GITHUB_BODY, GITHUB_BODY), bodies)
        assertEquals(2, mirrorAttempts())
        assertTrue(coolDownLogs().isEmpty())
    }

    @Test
    fun `mirror body that fails while it is read falls back to GitHub and pauses the mirror`() {
        backend.mirror = Answer.Status(200, bodyFailure = IOException("timeout"))

        val bodies = listOf(read(), read())

        assertEquals(listOf(GITHUB_BODY, GITHUB_BODY), bodies)
        assertTrue(backend.attempts.first().body!!.closed)
        assertEquals(1, mirrorAttempts())
    }

    @Test
    fun `call cancelled before it runs is not sent to the mirror`() {
        val call = client.newCall(get(CHAINS_URL))
        call.cancel()

        val thrown = runCatching { call.execute() }.exceptionOrNull()

        assertTrue(thrown is IOException)
        // The backend stands in front of OkHttp's own check, which fails such a call before any network use
        assertEquals(listOf(CHAINS_URL), backend.urls)
    }

    @Test
    fun `call cancelled during a failed mirror attempt is not sent to GitHub and does not pause the mirror`() {
        val mirrorFailure = IOException("connect timed out")
        backend.mirror = Answer.Failure(mirrorFailure)
        val call = client.newCall(get(CHAINS_URL))
        backend.onMirrorAttempt = { call.cancel() }

        val thrown = runCatching { call.execute() }.exceptionOrNull()
        mirrorRecovers()
        val afterwards = read()

        assertSame(mirrorFailure, thrown)
        assertEquals(MIRROR_BODY, afterwards)
        assertEquals(listOf(CHAINS_MIRROR_URL, CHAINS_MIRROR_URL), backend.urls)
        assertTrue(logger.messages.isEmpty())
    }

    @Test
    fun `cache-only request asks the mirror during a pause, then the GitHub url, and does not end the pause`() {
        mirrorFails()
        read()
        val cacheOnly = Request.Builder().url(CHAINS_URL).cacheControl(CacheControl.FORCE_CACHE).build()

        backend.mirror = Answer.Status(200, "[\"cached\"]", networkCode = null)
        val cachedUnderMirrorUrl = execute(cacheOnly).use { it.body!!.string() }
        // What OkHttp's cache answers a cache-only request it has no copy for
        backend.mirror = Answer.Status(504, networkCode = null)
        backend.gitHub = Answer.Status(200, "[\"cached from GitHub\"]", networkCode = null)
        backend.attempts.clear()
        val cachedUnderGitHubUrl = execute(cacheOnly).use { it.body!!.string() }
        val cacheOnlyAttempts = backend.attempts.toList()
        mirrorRecovers()
        backend.gitHub = Answer.Status(200, GITHUB_BODY)
        val afterwards = read()

        assertEquals("[\"cached\"]", cachedUnderMirrorUrl)
        assertEquals("[\"cached from GitHub\"]", cachedUnderGitHubUrl)
        assertEquals(listOf(CHAINS_MIRROR_URL, CHAINS_URL), cacheOnlyAttempts.map { it.request.url.toString() })
        assertTrue(cacheOnlyAttempts.all { it.request.cacheControl.onlyIfCached })
        assertEquals("the mirror is still paused", GITHUB_BODY, afterwards)
        assertEquals(1, coolDownLogs().size)
    }

    @Test
    fun `cool-down doubles while the mirror keeps failing and starts over after its answer`() {
        val coolDowns = listOf(COOL_DOWN_MILLIS, COOL_DOWN_MILLIS * 2, MAX_COOL_DOWN_MILLIS, MAX_COOL_DOWN_MILLIS)

        mirrorFails()
        coolDowns.forEach { coolDown ->
            read()
            elapsedRealtimeMillis += coolDown
        }
        mirrorRecovers()
        val recovered = listOf(read(), read())
        mirrorFails()
        read()

        assertEquals(listOf(MIRROR_BODY, MIRROR_BODY), recovered)
        assertEquals((coolDowns + COOL_DOWN_MILLIS).map { coolDownLog(it, "IOException") }, coolDownLogs())
    }

    @Test
    fun `one request probes the mirror after the cool-down while the others read GitHub`() {
        pauseMirrorAndWait()
        mirrorRecovers()
        var duringProbe = ""
        backend.onMirrorAttempt = {
            backend.onMirrorAttempt = {}
            duringProbe = read("$GITHUB_MASTER/a.json")
        }

        val probe = read()
        val afterProbe = read()

        assertEquals(listOf(MIRROR_BODY, GITHUB_BODY, MIRROR_BODY), listOf(probe, duringProbe, afterProbe))
    }

    @Test
    fun `probe answered with a status that does not pause the mirror ends the pause`() {
        val answers = listOf(
            Answer.Status(404),
            Answer.Status(302),
            Answer.Status(200, "<html>not a config</html>"),
            Answer.Status(200, MIRROR_BODY, commit = null),
            Answer.Status(200, MIRROR_BODY, networkCode = 304)
        )

        answers.forEach { answer ->
            pauseMirrorAndWait()
            backend.mirror = answer
            read()
            mirrorRecovers()
            backend.attempts.clear()

            val afterProbe = listOf(read(), read())

            assertEquals("after ${answer.code}", listOf(MIRROR_BODY, MIRROR_BODY), afterProbe)
        }
    }

    @Test
    fun `probe without a verdict leaves the probing to the next request`() {
        val gitHubUnreachable = Answer.Failure(IOException("github unreachable"))
        val withoutVerdict = listOf(
            Answer.Status(200, MIRROR_BODY, networkCode = null) to backend.gitHub,
            Answer.Failure(IOException("mirror unreachable")) to gitHubUnreachable
        )

        withoutVerdict.forEach { (mirrorAnswer, gitHubAnswer) ->
            pauseMirrorAndWait()
            backend.mirror = mirrorAnswer
            backend.gitHub = gitHubAnswer
            runCatching { read() }
            mirrorRecovers()
            backend.gitHub = Answer.Status(200, GITHUB_BODY)
            backend.attempts.clear()

            val nextRequest = read()

            assertEquals(MIRROR_BODY, nextRequest)
            assertEquals(listOf(CHAINS_MIRROR_URL), backend.urls)
        }
        assertEquals("no probe has made the pause longer", List(2) { coolDownLog(COOL_DOWN_MILLIS, "IOException") }, coolDownLogs())
    }

    @Test
    fun `failures of requests that were in flight together start one pause`() {
        mirrorFails()
        // Another request fails and starts the pause while the first mirror attempt is still in flight
        backend.onMirrorAttempt = {
            backend.onMirrorAttempt = {}
            read("$GITHUB_MASTER/a.json")
        }

        read()

        assertEquals(listOf(coolDownLog(COOL_DOWN_MILLIS, "IOException")), coolDownLogs())
    }

    @Test
    fun `mirror attempt has one deadline while the GitHub attempt keeps the request timeouts`() {
        val raisedTimeouts = Headers.Builder().add(TimeHeaderInterceptor.LONG_CONNECT).add(TimeHeaderInterceptor.LONG_READ).build()
        backend.mirror = Answer.Status(404)

        read()
        execute(Request.Builder().url("$GITHUB_MASTER/bittensor/v1/config.json").headers(raisedTimeouts).build()).close()

        val (mirrorAttempt, gitHubAttempt, longMirrorAttempt, longGitHubAttempt) = backend.attempts
        assertEquals(listOf(MAX_ATTEMPT_MILLIS, MAX_ATTEMPT_MILLIS), listOf(mirrorAttempt, longMirrorAttempt).map { it.callTimeoutMillis })
        assertEquals(listOf(NO_TIMEOUT_MILLIS, NO_TIMEOUT_MILLIS), listOf(gitHubAttempt, longGitHubAttempt).map { it.callTimeoutMillis })
        assertEquals(STANDARD_TIMEOUT_MILLIS to STANDARD_TIMEOUT_MILLIS, gitHubAttempt.connectTimeoutMillis to gitHubAttempt.readTimeoutMillis)
        assertEquals(LONG_TIMEOUT_MILLIS to LONG_TIMEOUT_MILLIS, longGitHubAttempt.connectTimeoutMillis to longGitHubAttempt.readTimeoutMillis)
        assertTrue("timeout headers must not reach the mirror", longMirrorAttempt.request.headers.size == 0)
    }

    @Test
    fun `mirror attempt deadline is at most half of the caller's shortest timeout`() {
        class Row(val callMillis: Long, val connectMillis: Long, val readMillis: Long, val deadlineMillis: Long)

        val shortMillis = MAX_ATTEMPT_MILLIS
        val rows = listOf(
            Row(NO_TIMEOUT_MILLIS, NO_TIMEOUT_MILLIS, NO_TIMEOUT_MILLIS, deadlineMillis = MAX_ATTEMPT_MILLIS),
            Row(NO_TIMEOUT_MILLIS, MAX_ATTEMPT_MILLIS * 2, MAX_ATTEMPT_MILLIS * 6, deadlineMillis = MAX_ATTEMPT_MILLIS),
            Row(shortMillis, MAX_ATTEMPT_MILLIS * 2, MAX_ATTEMPT_MILLIS * 2, deadlineMillis = shortMillis / 2),
            Row(NO_TIMEOUT_MILLIS, shortMillis, MAX_ATTEMPT_MILLIS * 2, deadlineMillis = shortMillis / 2),
            Row(NO_TIMEOUT_MILLIS, NO_TIMEOUT_MILLIS, shortMillis, deadlineMillis = shortMillis / 2),
            Row(NO_TIMEOUT_MILLIS, 1, shortMillis, deadlineMillis = 1)
        )

        val deadlines = rows.map { row ->
            val callerClient = client.newBuilder()
                .callTimeout(row.callMillis, TimeUnit.MILLISECONDS)
                .connectTimeout(row.connectMillis, TimeUnit.MILLISECONDS)
                .readTimeout(row.readMillis, TimeUnit.MILLISECONDS)
                .build()
            backend.attempts.clear()
            callerClient.newCall(get(CHAINS_URL)).execute().close()

            backend.attempts.single().callTimeoutMillis
        }

        assertEquals(rows.map { it.deadlineMillis }, deadlines)
    }

    private companion object {

        const val MIRROR_HOST = "mirror.test"

        const val MIRROR_BASE = "https://$MIRROR_HOST/v1/config/"

        const val GITHUB_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/master"

        const val GITHUB_REFS_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/refs/heads/master"

        const val CHAINS_URL = "$GITHUB_MASTER/chains/v23/chains.json"

        const val CHAINS_GITHUB_PATH = "/novasamatech/nova-utils/master/chains/v23/chains.json"

        const val CHAINS_MIRROR_URL = "${MIRROR_BASE}chains/v23/chains.json"

        const val MIRROR_BODY = "[\"mirror\"]"

        const val GITHUB_BODY = "[\"github\"]"

        const val COMMIT_HEADER = "X-Config-Commit"

        const val COMMIT = "0ad90bcf0cd8f61e4e10c3c4951368dc492e12db"

        val OTHER_URLS = listOf(
            "https://raw.githubusercontent.com/novasamatech/nova-wallet-android-releases/master/updates/v1/entrypoint_dev.json",
            "https://raw.githubusercontent.com/novasamatech/opengov-delegate-registry/master/registry/polkadot.json",
            "https://raw.githubusercontent.com/novasamatech/nova-utils/develop/chains/v23/chains.json",
            "https://tokens-price.test/api/v3/simple/price?ids=polkadot"
        )

        const val COOL_DOWN_MILLIS = 60_000L

        const val MAX_COOL_DOWN_MILLIS = COOL_DOWN_MILLIS * 4

        const val MAX_ATTEMPT_MILLIS = 10_000L

        const val MAX_BODY_BYTES = 64L

        const val STANDARD_TIMEOUT_MILLIS = 20_000

        const val LONG_TIMEOUT_MILLIS = 60_000

        const val NO_TIMEOUT_MILLIS = 0L
    }
}
