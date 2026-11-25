package io.novafoundation.nova.common.data.network

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.net.HttpURLConnection.HTTP_OK
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import okhttp3.Call
import okhttp3.Callback
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

// How often a waiting mirror attempt checks that its original call has not been cancelled.
private const val CANCELLATION_CHECK_INTERVAL_MILLIS = 50L

private const val LOG_TAG = "[NovaUtilsMirror]"

/**
 * A nova-utils `master` file on the config mirror.
 *
 * @param path the file's encoded repository path, such as `chains/v23/chains.json`.
 */
internal class NovaUtilsMirrorFile(val path: String, val url: HttpUrl)

/**
 * Maps the raw GitHub URL of a nova-utils `master` file to the same file on the config mirror:
 * [mirrorBaseUrl] followed by the file's repository path, unchanged. Query and fragment are not part of
 * the file's address and are dropped. Returns null for every other URL.
 */
internal fun novaUtilsMirrorFile(gitHubUrl: HttpUrl, mirrorBaseUrl: HttpUrl): NovaUtilsMirrorFile? {
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

    val mirrorUrl = mirrorBaseUrl.newBuilder()
        .apply { filePathSegments.forEach(::addEncodedPathSegment) }
        .build()

    return NovaUtilsMirrorFile(path = filePathSegments.joinToString("/"), url = mirrorUrl)
}

private fun List<String>.startsWith(prefix: List<String>): Boolean {
    return size >= prefix.size && subList(0, prefix.size) == prefix
}

/**
 * Reads nova-utils `master` files from the nova-infra config mirror and falls back to GitHub per request.
 *
 * Callers keep requesting the raw GitHub URL. A GET or HEAD for a nova-utils `master` file is first sent
 * to the mirror. Its 200 or 304 is the answer once the body has been read in full. Anything else - another
 * status, a redirect, a network failure, a body that fails or exceeds [maxBodySize], no complete answer
 * within [attemptDeadlineMillis] - makes the original GitHub request proceed as before, with its own
 * timeouts. A mirror 404 is not final: the file may be newer than the mirror's copy. Every other request
 * passes through untouched.
 *
 * The mirror attempt is a separate call on [mirrorCallFactory]. The timeouts of the original call apply to
 * each socket operation and to every address of the host in turn, so they cannot bound the attempt as a whole.
 * Cancelling the original call cancels the attempt. After the mirror could not be reached it is skipped for
 * [failureCoolDownMillis]; a status does not start this cool-down, it is quick and concerns one file.
 *
 * One instance serves every client it is registered on (the shared client and the image loader's client),
 * so they share the cool-down. A client without it keeps reading GitHub. It must stay an application
 * interceptor: it answers without proceeding.
 *
 * [mirrorBaseUrl] is a build constant and not `infraUrl` of the global config (see `InfrastructureUrls`),
 * because that config is itself a nova-utils file read through the mirror. Both must name the same host.
 *
 * @param mirrorCallFactory executes the mirror attempts; it must not contain this interceptor.
 * @param attemptDeadlineMillis total time for one mirror attempt: connecting, the headers and the body.
 * @param maxBodySize the largest mirror body, in bytes, held in memory.
 * @param elapsedRealtimeMillis monotonic clock of the cool-down.
 */
class NovaUtilsMirrorInterceptor(
    private val mirrorBaseUrl: HttpUrl,
    private val mirrorCallFactory: Call.Factory,
    private val attemptDeadlineMillis: Long,
    private val failureCoolDownMillis: Long,
    private val maxBodySize: Long,
    private val elapsedRealtimeMillis: () -> Long,
    private val logger: Logger
) : Interceptor {

    private val skipMirrorUntilMillis = AtomicLong(Long.MIN_VALUE)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mirrorFile = mirrorFileFor(request)
        if (mirrorFile == null || isCoolingDown()) return chain.proceed(request)

        return when (val outcome = attemptMirror(chain, request, mirrorFile)) {
            is MirrorOutcome.Answered -> outcome.response
            is MirrorOutcome.Declined -> {
                logFallback(mirrorFile, outcome.reason)
                chain.proceed(request)
            }
            is MirrorOutcome.Unreachable -> {
                logFallback(mirrorFile, outcome.failure.javaClass.simpleName)
                startCoolDown(outcome.failure)
                proceedAfterMirrorFailure(chain, request, outcome.failure)
            }
        }
    }

    private fun mirrorFileFor(request: Request): NovaUtilsMirrorFile? {
        if (request.method !in MIRRORED_METHODS) return null

        return novaUtilsMirrorFile(request.url, mirrorBaseUrl)
    }

    private fun attemptMirror(chain: Interceptor.Chain, request: Request, mirrorFile: NovaUtilsMirrorFile): MirrorOutcome {
        val mirrorRequest = request.newBuilder().url(mirrorFile.url).build()
        val mirrorCall = mirrorCallFactory.newCall(mirrorRequest)
        val attempt = MirrorAttempt(maxBodySize)
        mirrorCall.enqueue(attempt)

        val outcome = try {
            awaitUntilDeadline(chain, attempt)
        } catch (originalCallEnded: IOException) {
            mirrorCall.cancel()
            throw originalCallEnded
        }
        if (outcome != null) return outcome

        mirrorCall.cancel()
        val deadlineExceeded = SocketTimeoutException("No answer from the nova-utils mirror in $attemptDeadlineMillis ms")
        return MirrorOutcome.Unreachable(deadlineExceeded)
    }

    /**
     * Waits for the outcome of [attempt] and returns null when [attemptDeadlineMillis] has passed without one.
     * The wait ends at the deadline even while the mirror call cannot be interrupted, as in a DNS lookup.
     *
     * @throws IOException when the original call was cancelled or its thread interrupted.
     */
    private fun awaitUntilDeadline(chain: Interceptor.Chain, attempt: MirrorAttempt): MirrorOutcome? {
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(attemptDeadlineMillis)
        val checkIntervalNanos = TimeUnit.MILLISECONDS.toNanos(CANCELLATION_CHECK_INTERVAL_MILLIS)

        while (true) {
            if (chain.call().isCanceled()) throw IOException("Canceled")

            val remainingNanos = deadlineNanos - System.nanoTime()
            if (remainingNanos <= 0) return null

            val outcome = try {
                attempt.await(min(remainingNanos, checkIntervalNanos))
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException("Interrupted while waiting for the nova-utils mirror")
            }
            if (outcome != null) return outcome
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

    private fun logFallback(mirrorFile: NovaUtilsMirrorFile, reason: String) {
        logger.log("$LOG_TAG GitHub fallback for ${mirrorFile.path}: $reason")
    }

    private sealed interface MirrorOutcome {

        /** The mirror's 200 or 304 with the body already in memory. */
        class Answered(val response: Response) : MirrorOutcome

        /** The mirror was reached but gave no usable answer for this file. */
        class Declined(val reason: String) : MirrorOutcome

        /** The mirror could not be reached or did not deliver the whole answer in time. */
        class Unreachable(val failure: IOException) : MirrorOutcome
    }

    /**
     * Receives the result of a mirror call on the mirror client's thread and reads the body there, so that
     * a body that fails or stalls is known before the answer is handed to the caller.
     */
    private class MirrorAttempt(private val maxBodySize: Long) : Callback {

        private val finished = CountDownLatch(1)

        @Volatile
        private var outcome: MirrorOutcome? = null

        /** The outcome, or null when the attempt has not finished within [timeoutNanos]. */
        fun await(timeoutNanos: Long): MirrorOutcome? {
            finished.await(timeoutNanos, TimeUnit.NANOSECONDS)

            return outcome
        }

        override fun onFailure(call: Call, e: IOException) = finish(MirrorOutcome.Unreachable(e))

        override fun onResponse(call: Call, response: Response) {
            val outcome = try {
                response.use(::readOutcome)
            } catch (bodyFailure: IOException) {
                MirrorOutcome.Unreachable(bodyFailure)
            }

            finish(outcome)
        }

        private fun finish(outcome: MirrorOutcome) {
            this.outcome = outcome
            finished.countDown()
        }

        private fun readOutcome(response: Response): MirrorOutcome {
            // A 200 that ends a redirect chain is some other page, not the mirror's copy of the file.
            if (response.priorResponse != null) return MirrorOutcome.Declined("redirect")
            if (response.code !in MIRROR_ANSWER_CODES) return MirrorOutcome.Declined("status ${response.code}")

            val body = requireNotNull(response.body) { "The response of a call always has a body" }
            val bodyTooLarge = MirrorOutcome.Declined("body over $maxBodySize bytes")
            if (body.contentLength() > maxBodySize) return bodyTooLarge

            // Empty for HEAD and 304. One byte over the limit is enough to tell that the body exceeds it.
            val source = body.source()
            source.request(maxBodySize + 1)
            if (source.buffer.size > maxBodySize) return bodyTooLarge

            val content = Buffer()
            content.writeAll(source)
            val bufferedBody = content.asResponseBody(body.contentType(), content.size)

            return MirrorOutcome.Answered(response.newBuilder().body(bufferedBody).build())
        }
    }
}
