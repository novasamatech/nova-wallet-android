package io.novafoundation.nova.common.data.network

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer

private const val GITHUB_RAW_SCHEME = "https"
private const val GITHUB_RAW_HOST = "raw.githubusercontent.com"

// Raw GitHub accepts both spellings of the branch; only `master` is mirrored.
private val NOVA_UTILS_MASTER_PATH_PREFIXES = listOf(
    listOf("novasamatech", "nova-utils", "master"),
    listOf("novasamatech", "nova-utils", "refs", "heads", "master")
)

private val MIRRORED_METHODS = setOf("GET", "HEAD")

// The only statuses that are answers from the mirror's copy; everything else is read from GitHub.
private val MIRROR_ANSWER_CODES = setOf(HTTP_OK, HTTP_NOT_MODIFIED)

// OkHttp's value of a timeout that never expires.
private const val NO_TIMEOUT = 0

// Value of "skip until" while no cool-down is in force and every request may try the mirror.
private const val MIRROR_NOT_SKIPPED = Long.MIN_VALUE

private const val LOG_TAG = "[NovaUtilsMirror]"

/**
 * Maps the raw GitHub URL of a nova-utils `master` file to the same file on the config mirror:
 * [mirrorBaseUrl] followed by the file's repository path, unchanged. Query and fragment are not part of
 * the file's address and are dropped. Returns null for every other URL.
 */
internal fun novaUtilsMirrorUrl(gitHubUrl: HttpUrl, mirrorBaseUrl: HttpUrl): HttpUrl? {
    val isRawGitHub = gitHubUrl.scheme == GITHUB_RAW_SCHEME &&
        gitHubUrl.host == GITHUB_RAW_HOST &&
        gitHubUrl.port == HttpUrl.defaultPort(GITHUB_RAW_SCHEME)
    if (!isRawGitHub) return null

    val pathSegments = gitHubUrl.encodedPathSegments
    val masterPrefix = NOVA_UTILS_MASTER_PATH_PREFIXES.firstOrNull { pathSegments.startsWith(it) } ?: return null
    val filePathSegments = pathSegments.drop(masterPrefix.size)

    // An empty segment (a directory URL, `//`) cannot be appended to the base unchanged.
    val isFilePath = filePathSegments.isNotEmpty() && filePathSegments.none(String::isEmpty)
    if (!isFilePath) return null

    return mirrorBaseUrl.newBuilder()
        .apply { filePathSegments.forEach(::addEncodedPathSegment) }
        .build()
}

private fun List<String>.startsWith(prefix: List<String>): Boolean {
    return size >= prefix.size && subList(0, prefix.size) == prefix
}

/**
 * Reads nova-utils `master` files from the nova-infra config mirror and falls back to GitHub per request.
 *
 * Callers keep requesting the raw GitHub URL. A GET or HEAD for a nova-utils `master` file is first sent
 * to the mirror, on the same client and with capped socket timeouts. The mirror's own 200 or 304 is returned
 * once its body has been read. Any other status, a redirected answer, a network failure or a body that fails
 * while it is read makes the original GitHub request proceed as before, with its own timeouts. A mirror 404
 * is not final: the file may be newer than the mirror's copy. Every other request passes through untouched.
 *
 * A failed attempt that kept the request waiting for at least [mirrorConnectTimeoutMillis] starts a cool-down
 * during which the mirror is skipped. It lasts [failureCoolDownMillis] and doubles, up to
 * [maxFailureCoolDownMillis], each time the mirror fails that way again. When it ends, one request probes
 * the mirror while the others keep reading GitHub; the mirror's answer over the network ends it for all.
 * A quick failure (no connectivity, a refused connection) and a status only fall back. Cache-only requests
 * cost no network and are never skipped: files cached from the mirror are stored under its URL.
 *
 * One instance serves every client it is registered on (the shared client and the image loader's client),
 * so they share the cool-down; a client without it keeps reading GitHub. It must stay an application
 * interceptor: it proceeds twice and adjusts timeouts. [TimeHeaderInterceptor] must come before it,
 * otherwise it re-applies the header timeouts inside the mirror attempt and the cap is lost.
 *
 * Known limits, accepted for the sake of a simple interceptor:
 * - The timeouts bound each socket operation, not the mirror attempt as a whole. OkHttp tries every address
 *   of the mirror host in turn, so a request to an unreachable mirror waits up to one connect timeout plus
 *   one read timeout (the TLS handshake) per address before it falls back. Every request already in flight
 *   when the mirror stops answering pays that wait once. A caller that gives up earlier cancels the call,
 *   which does not fall back; its retry does, because the cool-down has started.
 * - The cool-down lives in memory. After a cold start the mirror is tried by the first requests again.
 * - The mirror body is held in memory before it is returned; nova-utils files and icons are small.
 *
 * [mirrorBaseUrl] is a build constant and not `infraUrl` of the global config (see `InfrastructureUrls`),
 * because that config is itself a nova-utils file read through the mirror. Both must name the same host.
 *
 * @param mirrorConnectTimeoutMillis cap of the connect timeout of a mirror attempt.
 * @param mirrorReadTimeoutMillis cap of the read timeout of a mirror attempt. A longer timeout of the request,
 * such as one raised through [TimeHeaderInterceptor], applies to the GitHub attempt only.
 * @param elapsedRealtimeMillis monotonic clock of the cool-down.
 */
class NovaUtilsMirrorInterceptor(
    private val mirrorBaseUrl: HttpUrl,
    private val mirrorConnectTimeoutMillis: Int,
    private val mirrorReadTimeoutMillis: Int,
    private val failureCoolDownMillis: Long,
    private val maxFailureCoolDownMillis: Long,
    private val elapsedRealtimeMillis: () -> Long,
    private val logger: Logger
) : Interceptor {

    private val skipMirrorUntilMillis = AtomicLong(MIRROR_NOT_SKIPPED)

    private val nextCoolDownMillis = AtomicLong(failureCoolDownMillis)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mirrorRequest = mirrorRequestFor(request) ?: return chain.proceed(request)
        val admission = admitMirrorAttempt(request) ?: return chain.proceed(request)

        val startedMillis = elapsedRealtimeMillis()
        val mirrorResponse = try {
            val response = proceedWithMirror(chain, mirrorRequest)
            if (rejectionReasonOf(response) == null) response.withBodyInMemory() else response
        } catch (mirrorFailure: IOException) {
            // Only a failure that kept the request waiting is worth avoiding next time.
            val keptWaiting = elapsedRealtimeMillis() - startedMillis >= mirrorConnectTimeoutMillis
            if (keptWaiting) startCoolDown(admission, mirrorFailure)

            // A cancelled call fails here as well; it must not start another request.
            if (chain.call().isCanceled()) throw mirrorFailure

            logFallback(request, reason = mirrorFailure.javaClass.simpleName)
            return proceedAfterMirrorFailure(chain, request, mirrorFailure)
        }

        val rejectionReason = rejectionReasonOf(mirrorResponse)
        if (rejectionReason == null) {
            // An answer from the cache says nothing about the mirror being reachable.
            if (mirrorResponse.networkResponse != null) endCoolDown()
            return mirrorResponse
        }

        // OkHttp refuses to proceed again while the previous response is still open.
        mirrorResponse.close()
        logFallback(request, rejectionReason)
        return chain.proceed(request)
    }

    private fun mirrorRequestFor(request: Request): Request? {
        if (request.method !in MIRRORED_METHODS) return null
        val mirrorUrl = novaUtilsMirrorUrl(request.url, mirrorBaseUrl) ?: return null

        return request.newBuilder().url(mirrorUrl).build()
    }

    /**
     * The "skip until" value under which [request] may try the mirror, or null while the mirror is skipped.
     */
    private fun admitMirrorAttempt(request: Request): Long? {
        val skipUntilMillis = skipMirrorUntilMillis.get()
        if (skipUntilMillis == MIRROR_NOT_SKIPPED || request.cacheControl.onlyIfCached) return skipUntilMillis

        val now = elapsedRealtimeMillis()
        if (now < skipUntilMillis) return null

        // The cool-down has ended. The request that moves it forward probes the mirror; the others keep reading
        // GitHub until the probe has reported back, which takes less than a cool-down.
        val probeUntilMillis = now + failureCoolDownMillis
        return probeUntilMillis.takeIf { skipMirrorUntilMillis.compareAndSet(skipUntilMillis, it) }
    }

    private fun proceedWithMirror(chain: Interceptor.Chain, mirrorRequest: Request): Response {
        val connectTimeoutMillis = cappedTimeout(chain.connectTimeoutMillis(), mirrorConnectTimeoutMillis)
        val readTimeoutMillis = cappedTimeout(chain.readTimeoutMillis(), mirrorReadTimeoutMillis)

        return chain
            .withConnectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .withReadTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
            .proceed(mirrorRequest)
    }

    private fun cappedTimeout(requestTimeoutMillis: Int, capMillis: Int): Int {
        return if (requestTimeoutMillis == NO_TIMEOUT) capMillis else min(requestTimeoutMillis, capMillis)
    }

    /**
     * Reads the body now, while a failure can still be answered by GitHub. It is empty for HEAD and 304.
     */
    private fun Response.withBodyInMemory(): Response {
        val body = body ?: return this
        val content = Buffer()
        body.use { content.writeAll(it.source()) }

        return newBuilder().body(content.asResponseBody(body.contentType(), content.size)).build()
    }

    /**
     * Why [mirrorResponse] is not the mirror's copy of the file, or null when it is.
     */
    private fun rejectionReasonOf(mirrorResponse: Response): String? {
        return when {
            // A 200 that ends a redirect chain is some other page.
            mirrorResponse.priorResponse != null -> "redirect"
            mirrorResponse.code !in MIRROR_ANSWER_CODES -> "status ${mirrorResponse.code}"
            else -> null
        }
    }

    private fun proceedAfterMirrorFailure(chain: Interceptor.Chain, request: Request, mirrorFailure: IOException): Response {
        try {
            return chain.proceed(request)
        } catch (gitHubFailure: IOException) {
            gitHubFailure.addSuppressed(mirrorFailure)
            throw gitHubFailure
        }
    }

    /**
     * @param admission the value [admitMirrorAttempt] gave the failed attempt. Requests that were in flight
     * together fail one after another; only the first of them starts the cool-down and makes the next one longer.
     */
    private fun startCoolDown(admission: Long, mirrorFailure: IOException) {
        val coolDownMillis = nextCoolDownMillis.get()
        val skipUntilMillis = elapsedRealtimeMillis() + coolDownMillis
        if (!skipMirrorUntilMillis.compareAndSet(admission, skipUntilMillis)) return

        nextCoolDownMillis.set(min(coolDownMillis * 2, maxFailureCoolDownMillis))
        logger.log("$LOG_TAG Mirror skipped for $coolDownMillis ms after ${mirrorFailure.javaClass.simpleName}")
    }

    private fun endCoolDown() {
        skipMirrorUntilMillis.set(MIRROR_NOT_SKIPPED)
        nextCoolDownMillis.set(failureCoolDownMillis)
    }

    private fun logFallback(request: Request, reason: String) {
        logger.log("$LOG_TAG GitHub fallback for ${request.url.encodedPath}: $reason")
    }
}
