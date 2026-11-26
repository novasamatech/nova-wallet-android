package io.novafoundation.nova.common.data.network

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.Closeable
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a backend in place of the network cannot show: the mirror attempt on the real OkHttp engine, against a
 * mirror on a local socket and with a real HTTP cache, assembled the way the app does it, by [NovaUtilsMirror.addTo].
 * GitHub is still answered in place of the network, as its URL cannot be served locally.
 */
class NovaUtilsMirrorRealEngineTest {

    private enum class Stall { NONE, BEFORE_ANSWER, IN_BODY }

    /**
     * @param commit value of the `X-Config-Commit` header, which the mirror service puts on its answers, or null
     * for an answer without it.
     */
    private class Reply(
        val status: Int,
        val headers: Map<String, String> = emptyMap(),
        val body: String = "",
        val stall: Stall = Stall.NONE,
        val commit: String? = COMMIT
    )

    private class MirrorRequest(val path: String, val headers: Map<String, String>)

    /** A mirror on a local socket: one request per connection, answered by [reply]. */
    private class LocalMirror : Closeable {

        private val serverSocket = ServerSocket(0, BACKLOG, InetAddress.getLoopbackAddress())

        private val connections = CopyOnWriteArrayList<Socket>()

        val requests = CopyOnWriteArrayList<MirrorRequest>()

        @Volatile
        var reply: (MirrorRequest) -> Reply = { Reply(200, body = MIRROR_BODY) }

        val baseUrl get() = "http://${serverSocket.inetAddress.hostAddress}:${serverSocket.localPort}/v1/config/"

        val paths get() = requests.map { it.path }

        init {
            thread(isDaemon = true) {
                runCatching {
                    while (true) {
                        val connection = serverSocket.accept()
                        connections += connection
                        thread(isDaemon = true) { runCatching { serve(connection) } }
                    }
                }
            }
        }

        private fun serve(connection: Socket) {
            val head = connection.getInputStream().bufferedReader(Charsets.ISO_8859_1)
            val path = head.readLine().split(" ")[1]
            val headers = generateSequence { head.readLine().takeIf(String::isNotEmpty) }
                .associate { line -> line.substringBefore(":").lowercase() to line.substringAfter(":").trim() }
            val request = MirrorRequest(path, headers)
            requests += request

            val reply = reply(request)
            // A stalled connection stays open until the mirror is closed
            if (reply.stall == Stall.BEFORE_ANSWER) return

            val bodyBytes = reply.body.toByteArray()
            val commitHeader = reply.commit?.let { mapOf(COMMIT_HEADER to it) }.orEmpty()
            val replyHeaders = reply.headers + commitHeader + mapOf("Content-Length" to bodyBytes.size.toString(), "Connection" to "close")
            val replyHead = "HTTP/1.1 ${reply.status} Test\r\n" + replyHeaders.entries.joinToString("") { "${it.key}: ${it.value}\r\n" } + "\r\n"
            val output = connection.getOutputStream()
            output.write(replyHead.toByteArray(Charsets.ISO_8859_1))
            val sentBodyBytes = if (reply.stall == Stall.IN_BODY) bodyBytes.size / 2 else bodyBytes.size
            output.write(bodyBytes, 0, sentBodyBytes)
            output.flush()
            if (reply.stall == Stall.NONE) connection.close()
        }

        override fun close() {
            serverSocket.close()
            connections.forEach(Socket::close)
        }
    }

    /** GitHub over "the network": it has every file, and no cached copy for a cache-only request. */
    private class GitHub : Interceptor {

        val requests = CopyOnWriteArrayList<Request>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            val response = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).message("test")
            if (request.cacheControl.onlyIfCached) return response.code(504).body("".toResponseBody()).build()

            val networkResponse = response.code(200).build()

            return response.body(GITHUB_BODY.toResponseBody()).networkResponse(networkResponse).build()
        }
    }

    private class SilentLogger : Logger {

        override fun log(message: String?) = Unit

        override fun log(throwable: Throwable?) = Unit
    }

    private val mirror = LocalMirror()

    private val gitHub = GitHub()

    private val logger = SilentLogger()

    private val cacheDirectory: File = Files.createTempDirectory("nova-utils-mirror-test").toFile()

    private val coolDown = NovaUtilsMirrorCoolDown(initialMillis = COOL_DOWN_MILLIS, maxMillis = COOL_DOWN_MILLIS, clock = { 0L })

    private val novaUtilsMirror = NovaUtilsMirror(
        mirrorBaseUrl = mirror.baseUrl.toHttpUrl(),
        maxAttemptMillis = ATTEMPT_DEADLINE_MILLIS,
        maxBodyBytes = MAX_BODY_BYTES,
        coolDown = coolDown,
        logger = logger
    )

    private val appClient = OkHttpClient.Builder()
        .connectTimeout(SOCKET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .readTimeout(SOCKET_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .cache(Cache(cacheDirectory, CACHE_BYTES))
        .addInterceptor(TimeHeaderInterceptor())
        .build()

    private val client = novaUtilsMirror.addTo(appClient)
        .newBuilder()
        .addInterceptor(gitHub)
        .build()

    @After
    fun tearDown() {
        mirror.close()
        appClient.connectionPool.evictAll()
        cacheDirectory.deleteRecursively()
    }

    private fun get(url: String = CHAINS_URL, cacheControl: CacheControl? = null): Response {
        val request = Request.Builder().url(url).apply { cacheControl?.let(::cacheControl) }.build()

        return client.newCall(request).execute()
    }

    private fun read(url: String = CHAINS_URL): String = get(url).use { it.body!!.string() }

    private fun <T> timed(action: () -> T): Pair<T, Long> {
        val startedNanos = System.nanoTime()
        val result = action()

        return result to TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos)
    }

    private fun assertGaveUpAtTheDeadline(elapsedMillis: Long) {
        assertTrue("gave up after $elapsedMillis ms, before the deadline", elapsedMillis >= ATTEMPT_DEADLINE_MILLIS)
        assertTrue("gave up after $elapsedMillis ms, by a socket timeout", elapsedMillis < SOCKET_TIMEOUT_MILLIS / 2)
    }

    @Test
    fun `mirror on the real engine is read at the mapped path and GitHub is not asked`() {
        val body = read()

        assertEquals(MIRROR_BODY, body)
        assertEquals(listOf(CHAINS_MIRROR_PATH), mirror.paths)
        assertTrue(gitHub.requests.isEmpty())
    }

    @Test
    fun `mirror that accepts the connection and never answers is given up at the deadline and paused`() {
        mirror.reply = { Reply(200, stall = Stall.BEFORE_ANSWER) }

        val (body, elapsedMillis) = timed { read() }
        val afterwards = read()

        assertEquals(listOf(GITHUB_BODY, GITHUB_BODY), listOf(body, afterwards))
        assertGaveUpAtTheDeadline(elapsedMillis)
        assertEquals("the mirror must be skipped by the second request", 1, mirror.requests.size)
    }

    @Test
    fun `mirror body that stalls is given up at the same deadline`() {
        mirror.reply = { Reply(200, body = MIRROR_BODY, stall = Stall.IN_BODY) }

        val (body, elapsedMillis) = timed { read() }

        assertEquals(GITHUB_BODY, body)
        assertGaveUpAtTheDeadline(elapsedMillis)
    }

    @Test
    fun `redirect from the mirror is not followed and does not pause it`() {
        mirror.reply = { request ->
            if (request.path == CHAINS_MIRROR_PATH) Reply(302, headers = mapOf("Location" to "/v1/config/moved.json")) else Reply(200, body = MIRROR_BODY)
        }

        val bodies = listOf(read(), read())

        assertEquals(listOf(GITHUB_BODY, GITHUB_BODY), bodies)
        assertEquals(listOf(CHAINS_MIRROR_PATH, CHAINS_MIRROR_PATH), mirror.paths)
    }

    @Test
    fun `file cached from the mirror is served to a cache-only request during a pause`() {
        val otherFileUrl = "$GITHUB_MASTER/chains/v23/chains_dev.json"
        val neverCachedUrl = "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg"
        mirror.reply = { request ->
            if (request.path == CHAINS_MIRROR_PATH) Reply(200, headers = mapOf("Cache-Control" to "public, max-age=325"), body = MIRROR_BODY) else Reply(503)
        }
        read()
        read(otherFileUrl)
        val mirrorRequestsBefore = mirror.requests.size

        val (cached, cachedCommit) = get(cacheControl = CacheControl.FORCE_CACHE).use { it.body!!.string() to it.header(COMMIT_HEADER) }
        val neverCached = get(neverCachedUrl, CacheControl.FORCE_CACHE).use { it.code }
        val overTheNetwork = read(neverCachedUrl)

        assertEquals(MIRROR_BODY, cached)
        assertEquals("the cached copy keeps the header it is accepted by", COMMIT, cachedCommit)
        assertEquals("a file with no cached copy is the cache's own refusal, as without the mirror", 504, neverCached)
        assertEquals("the mirror is paused for requests that need the network", GITHUB_BODY, overTheNetwork)
        assertEquals(mirrorRequestsBefore, mirror.requests.size)
    }

    @Test
    fun `200 without the commit header is read from GitHub, also when the cache has stored it, and does not pause the mirror`() {
        val otherFileUrl = "$GITHUB_MASTER/chains/v23/chains_dev.json"
        mirror.reply = { request ->
            val isIcon = request.path == ICON_MIRROR_PATH
            if (isIcon) Reply(200, mapOf("Cache-Control" to "public, max-age=325"), "<html>Blocked</html>", commit = null) else Reply(200, body = MIRROR_BODY)
        }

        val overTheNetwork = read(ICON_URL)
        val fromTheStoredPage = read(ICON_URL)
        val cacheOnly = get(ICON_URL, CacheControl.FORCE_CACHE).use { it.code }
        val otherFile = read(otherFileUrl)

        assertEquals(listOf(GITHUB_BODY, GITHUB_BODY), listOf(overTheNetwork, fromTheStoredPage))
        assertEquals("the stored page must not be served to a cache-only request either", 504, cacheOnly)
        assertEquals("the mirror is not paused", MIRROR_BODY, otherFile)
        assertEquals("the page was stored: the mirror is asked for it once", 1, mirror.paths.count { it == ICON_MIRROR_PATH })
    }

    @Test
    fun `stale cached copy that the mirror confirms with 304 is returned as its 200 answer`() {
        mirror.reply = { request ->
            val isRevalidation = request.headers["if-none-match"] == ETAG
            val headers = mapOf("Cache-Control" to "public, max-age=0", "ETag" to ETAG)
            // The copy is accepted by the header it was stored with, whatever the 304 carries
            if (isRevalidation) Reply(304, headers, commit = null) else Reply(200, headers, MIRROR_BODY)
        }

        val first = get().use { it.code to it.body!!.string() }
        val revalidated = get().use { it.code to it.body!!.string() }

        assertEquals(listOf(200 to MIRROR_BODY, 200 to MIRROR_BODY), listOf(first, revalidated))
        assertEquals(listOf(null, ETAG), mirror.requests.map { it.headers["if-none-match"] })
        assertTrue(gitHub.requests.isEmpty())
    }

    private companion object {

        const val GITHUB_MASTER = "https://raw.githubusercontent.com/novasamatech/nova-utils/master"

        const val CHAINS_URL = "$GITHUB_MASTER/chains/v23/chains.json"

        const val CHAINS_MIRROR_PATH = "/v1/config/chains/v23/chains.json"

        const val ICON_URL = "$GITHUB_MASTER/icons/tokens/white/v1/DOT.svg"

        const val ICON_MIRROR_PATH = "/v1/config/icons/tokens/white/v1/DOT.svg"

        const val COMMIT_HEADER = "X-Config-Commit"

        const val COMMIT = "0ad90bcf0cd8f61e4e10c3c4951368dc492e12db"

        const val MIRROR_BODY = "[\"mirror\"]"

        const val GITHUB_BODY = "[\"github\"]"

        const val ETAG = "\"v1\""

        const val ATTEMPT_DEADLINE_MILLIS = 500L

        // Far above the deadline: an attempt that ends near it was ended by the deadline
        const val SOCKET_TIMEOUT_MILLIS = 30_000L

        const val COOL_DOWN_MILLIS = 60_000L

        const val MAX_BODY_BYTES = 1024L

        const val CACHE_BYTES = 1024L * 1024L

        const val BACKLOG = 50
    }
}
