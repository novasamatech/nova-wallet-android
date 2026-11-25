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
 * to the mirror, on the same client and with capped socket timeouts. The mirror's own 200 or 304 is returned.
 * Any other status, a redirected answer or a network failure makes the original GitHub request proceed as
 * before, with its own timeouts. A mirror 404 is not final: the file may be newer than the mirror's copy.
 * Every other request passes through untouched.
 *
 * After a network failure the mirror is skipped for [failureCoolDownMillis]. A status does not start this
 * cool-down: it is quick and concerns one file. One instance serves every client it is registered on (the
 * shared client and the image loader's client), so they share the cool-down; a client without it keeps
 * reading GitHub. It must stay an application interceptor: it proceeds twice and adjusts timeouts.
 *
 * Known limits, accepted for the sake of a simple interceptor:
 * - The timeouts bound each socket operation, not the mirror attempt as a whole. OkHttp tries every address
 *   of the mirror host in turn, so the first request to an unreachable mirror waits one connect timeout per
 *   address before it falls back - or one read timeout per address when they accept the connection and then
 *   stall the TLS handshake. The cool-down keeps later requests from repeating that wait. A caller that gives
 *   up earlier cancels the call, which neither falls back nor starts the cool-down.
 * - Only the status line decides. A mirror 200 whose body then fails while it is being read is an error for
 *   that request, and [mirrorReadTimeoutMillis] stays in force for that body.
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
    private val elapsedRealtimeMillis: () -> Long,
    private val logger: Logger
) : Interceptor {

    private val skipMirrorUntilMillis = AtomicLong(Long.MIN_VALUE)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mirrorRequest = mirrorRequestFor(request)
        if (mirrorRequest == null || isCoolingDown()) return chain.proceed(request)

        val mirrorResponse = try {
            proceedWithMirror(chain, mirrorRequest)
        } catch (mirrorFailure: IOException) {
            // A cancelled call fails here as well; it must not start another request.
            if (chain.call().isCanceled()) throw mirrorFailure

            logFallback(request, reason = mirrorFailure.javaClass.simpleName)
            startCoolDown(mirrorFailure)
            return proceedAfterMirrorFailure(chain, request, mirrorFailure)
        }

        val rejectionReason = rejectionReasonOf(mirrorResponse) ?: return mirrorResponse

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

    private fun isCoolingDown(): Boolean = elapsedRealtimeMillis() < skipMirrorUntilMillis.get()

    private fun startCoolDown(mirrorFailure: IOException) {
        val now = elapsedRealtimeMillis()
        val previousSkipUntil = skipMirrorUntilMillis.getAndSet(now + failureCoolDownMillis)

        // Requests already in flight when the mirror went down fail one after another; announce it once.
        val wasCoolingDown = now < previousSkipUntil
        if (!wasCoolingDown) {
            logger.log("$LOG_TAG Mirror skipped for $failureCoolDownMillis ms after ${mirrorFailure.javaClass.simpleName}")
        }
    }

    private fun logFallback(request: Request, reason: String) {
        logger.log("$LOG_TAG GitHub fallback for ${request.url.encodedPath}: $reason")
    }
}
