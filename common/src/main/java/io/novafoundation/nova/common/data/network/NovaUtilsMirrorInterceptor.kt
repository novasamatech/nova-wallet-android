package io.novafoundation.nova.common.data.network

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import io.novafoundation.nova.common.data.network.NovaUtilsMirrorCoolDown.Admission
import io.novafoundation.nova.common.data.network.NovaUtilsMirrorCoolDown.Outcome
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer

private const val GITHUB_RAW_SCHEME = "https"
private const val GITHUB_RAW_HOST = "raw.githubusercontent.com"

// Raw GitHub accepts both spellings of the branch; only `master` is mirrored.
private val NOVA_UTILS_MASTER_PATH_PREFIXES = listOf(
    listOf("novasamatech", "nova-utils", "master"),
    listOf("novasamatech", "nova-utils", "refs", "heads", "master")
)

private val DOT_SEGMENTS = setOf(".", "..")

// What a percent-encoded separator decodes to; OkHttp reads a backslash in a path as a slash.
private val DECODED_PATH_SEPARATORS = charArrayOf('/', '\\')

// A mirror answer needs a body, so HEAD is left to GitHub.
private const val MIRRORED_METHOD = "GET"

private const val JSON_FILE_SUFFIX = ".json"

// The mirror service puts it on every 200. Only its presence is checked; the commit is not read or compared.
private const val MIRROR_COMMIT_HEADER = "X-Config-Commit"

private const val HTTP_TOO_MANY_REQUESTS = 429

// OkHttp's value of a timeout that never expires.
private const val NO_TIMEOUT = 0L

// A mirror attempt may take at most this part of the caller's own timeout.
private const val CALLER_TIMEOUT_DIVISOR = 2

private const val MIN_ATTEMPT_DEADLINE_MILLIS = 1L

private const val LOG_TAG = "[NovaUtilsMirror]"

/**
 * Maps the raw GitHub URL of a nova-utils `master` file to the same file on the config mirror:
 * [mirrorBaseUrl] followed by the file's repository path, unchanged. Query and fragment are not part of
 * the file's address and are dropped. Returns null for every other URL, and for a path with a dot segment.
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

    // The mirror decodes the path once and does not normalize it. OkHttp resolves `..` and `%2e%2e` segments
    // itself while it parses a URL, but not those that appear only after decoding, such as `..%2F`.
    val decodedFilePathSegments = gitHubUrl.pathSegments.drop(masterPrefix.size)
    if (decodedFilePathSegments.any(String::hasDotSegment)) return null

    return mirrorBaseUrl.newBuilder()
        .apply { filePathSegments.forEach(::addEncodedPathSegment) }
        .build()
}

private fun List<String>.startsWith(prefix: List<String>): Boolean {
    return size >= prefix.size && subList(0, prefix.size) == prefix
}

private fun String.hasDotSegment(): Boolean {
    return split(*DECODED_PATH_SEPARATORS).any { it in DOT_SEGMENTS }
}

/**
 * The part of [NovaUtilsMirror] that sits on one client: maps the URL, runs the mirror attempt under its
 * deadline and falls back to GitHub. When to skip the mirror is decided by [coolDown], which the clients share.
 *
 * The mirror attempt is a call of its own on [mirrorClient], because only a call has a timeout that spans
 * every address of the host, the TLS handshakes, the headers and the body; the timeouts of a chain bound each
 * socket operation separately. Its body is read completely, at most [maxBodyBytes], before anything is returned,
 * so that a failure at any point can still be answered by GitHub. The GitHub request proceeds on the chain,
 * untouched and with the caller's timeouts.
 *
 * It must be an application interceptor: it answers without proceeding or proceeds after another call.
 *
 * Limits of OkHttp, accepted:
 * - A caller that cancels its call is noticed when the mirror attempt returns, at most one deadline later: the
 *   cancellation of a call cannot be observed from an interceptor. Nothing is returned to that caller.
 * - The deadline cannot interrupt a DNS lookup that hangs; the attempt ends when the lookup does.
 *
 * @param mirrorClient the client this interceptor is registered on, without the interceptor and with
 * redirects off. It carries that client's cache, so files cached from the mirror are stored under its URL.
 * @param maxAttemptMillis the deadline of a mirror attempt, see [attemptDeadlineMillis].
 */
internal class NovaUtilsMirrorInterceptor(
    private val mirrorBaseUrl: HttpUrl,
    private val mirrorClient: OkHttpClient,
    private val maxAttemptMillis: Long,
    private val maxBodyBytes: Long,
    private val coolDown: NovaUtilsMirrorCoolDown,
    private val logger: Logger
) : Interceptor {

    private sealed interface MirrorAttempt {

        class Accepted(val response: Response, val outcome: Outcome) : MirrorAttempt

        /** The mirror gave an answer that is not its copy of the file. */
        class Rejected(val reason: String, val outcome: Outcome) : MirrorAttempt

        /** No complete answer: a network or TLS error, the deadline, a body that broke off. */
        class Failed(val failure: IOException) : MirrorAttempt
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mirrorRequest = mirrorRequestFor(request)
        // A call cancelled while it was queued must not cost a mirror request; proceeding fails it as usual.
        if (mirrorRequest == null || chain.call().isCanceled()) return chain.proceed(request)

        val admission = coolDown.admit(cacheOnly = request.cacheControl.onlyIfCached)
        if (admission == Admission.Skipped) return chain.proceed(request)

        val attempt = runCatching { attemptMirror(chain, mirrorRequest) }.getOrElse { unexpected ->
            // Not a verdict about the mirror; a probe must still give its slot back.
            coolDown.report(admission, Outcome.NO_VERDICT)
            throw unexpected
        }

        return when (attempt) {
            is MirrorAttempt.Accepted -> {
                coolDown.report(admission, attempt.outcome)
                attempt.response
            }
            is MirrorAttempt.Rejected -> {
                logFallback(request, attempt.reason)
                report(admission, attempt.outcome, attempt.reason)
                chain.proceed(request)
            }
            is MirrorAttempt.Failed -> fallBackAfterFailure(chain, request, admission, attempt.failure)
        }
    }

    private fun mirrorRequestFor(request: Request): Request? {
        if (request.method != MIRRORED_METHOD) return null
        val mirrorUrl = novaUtilsMirrorUrl(request.url, mirrorBaseUrl) ?: return null

        return request.newBuilder().url(mirrorUrl).build()
    }

    private fun attemptMirror(chain: Interceptor.Chain, mirrorRequest: Request): MirrorAttempt {
        val mirrorCall = mirrorClient.newCall(mirrorRequest)
        mirrorCall.timeout().timeout(attemptDeadlineMillis(chain), TimeUnit.MILLISECONDS)

        return try {
            // The deadline keeps running until the body has been read.
            mirrorCall.execute().use(::acceptOrReject)
        } catch (failure: IOException) {
            MirrorAttempt.Failed(failure)
        }
    }

    /**
     * [maxAttemptMillis], and never more than half of the caller's own timeout: the smallest of the timeouts
     * the caller has set for the call and its socket operations. A coroutine deadline around the call is not
     * visible here.
     */
    private fun attemptDeadlineMillis(chain: Interceptor.Chain): Long {
        val callerTimeoutsMillis = listOf(
            TimeUnit.NANOSECONDS.toMillis(chain.call().timeout().timeoutNanos()),
            chain.connectTimeoutMillis().toLong(),
            chain.readTimeoutMillis().toLong()
        )
        val callerTimeoutMillis = callerTimeoutsMillis.filter { it != NO_TIMEOUT }.minOrNull() ?: return maxAttemptMillis
        val deadlineMillis = min(maxAttemptMillis, callerTimeoutMillis / CALLER_TIMEOUT_DIVISOR)

        // Zero would mean no deadline at all.
        return max(deadlineMillis, MIN_ATTEMPT_DEADLINE_MILLIS)
    }

    /**
     * The mirror's copy of the file is a 200 that carries [MIRROR_COMMIT_HEADER] and a body, which for a `.json`
     * file is JSON. The header tells the mirror service from a page that something on the way answers with a 200,
     * which for an icon could not be told by its body. A 304 that revalidates a cached copy never gets here as
     * such: OkHttp's cache returns that copy as a 200, with the headers it was stored with.
     */
    private fun acceptOrReject(mirrorResponse: Response): MirrorAttempt {
        val outcome = outcomeOf(mirrorResponse)
        if (mirrorResponse.code != HTTP_OK) return MirrorAttempt.Rejected("status ${mirrorResponse.code}", outcome)

        val isFromMirrorService = !mirrorResponse.header(MIRROR_COMMIT_HEADER).isNullOrBlank()
        if (!isFromMirrorService) return MirrorAttempt.Rejected("no $MIRROR_COMMIT_HEADER header", outcome)

        val body = mirrorResponse.body ?: return MirrorAttempt.Rejected("no body", outcome)
        val content = body.readAtMost(maxBodyBytes)
        val isJsonFile = mirrorResponse.request.url.encodedPath.endsWith(JSON_FILE_SUFFIX, ignoreCase = true)
        val rejectionReason = when {
            content == null -> "body over $maxBodyBytes bytes"
            content.size == 0L -> "empty body"
            isJsonFile && !content.isJson() -> "body is not JSON"
            else -> return MirrorAttempt.Accepted(mirrorResponse.withBody(content, body), outcome)
        }

        return MirrorAttempt.Rejected(rejectionReason, outcome)
    }

    /**
     * What [mirrorResponse] says about the mirror as a whole, whether or not it is usable for this file.
     */
    private fun outcomeOf(mirrorResponse: Response): Outcome {
        // An answer of the local cache: a cached file, or the refusal of a cache-only request.
        val networkStatus = mirrorResponse.networkResponse?.code ?: return Outcome.NO_VERDICT
        val isMirrorWideFailure = networkStatus >= HTTP_INTERNAL_ERROR || networkStatus == HTTP_TOO_MANY_REQUESTS

        return if (isMirrorWideFailure) Outcome.FAILED else Outcome.ANSWERED
    }

    /**
     * The whole body, or null when it is longer than [maxBytes].
     */
    private fun ResponseBody.readAtMost(maxBytes: Long): Buffer? {
        val source = source()
        val isOverLimit = source.request(maxBytes + 1)
        if (isOverLimit) return null

        return Buffer().apply { writeAll(source) }
    }

    private fun Buffer.isJson(): Boolean {
        return runCatching {
            JsonReader(InputStreamReader(peek().inputStream(), Charsets.UTF_8)).use { reader ->
                reader.skipValue()
                reader.peek() == JsonToken.END_DOCUMENT
            }
        }.getOrDefault(false)
    }

    private fun Response.withBody(content: Buffer, originalBody: ResponseBody): Response {
        return newBuilder().body(content.asResponseBody(originalBody.contentType(), content.size)).build()
    }

    /**
     * A failed attempt pauses the mirror only when GitHub then answers over the network: the errors of a device
     * without connectivity look the same as the errors of an unreachable mirror.
     */
    private fun fallBackAfterFailure(chain: Interceptor.Chain, request: Request, admission: Admission, mirrorFailure: IOException): Response {
        // The caller has gone: nothing to fall back for, and no reason to pause the mirror.
        if (chain.call().isCanceled()) {
            coolDown.report(admission, Outcome.NO_VERDICT)
            throw mirrorFailure
        }

        val reason = mirrorFailure.javaClass.simpleName
        logFallback(request, reason)
        val gitHubAttempt = runCatching { chain.proceed(request) }
        val gitHubAnswered = gitHubAttempt.getOrNull()?.networkResponse != null
        val outcome = if (gitHubAnswered) Outcome.FAILED else Outcome.NO_VERDICT
        report(admission, outcome, reason)

        return gitHubAttempt.getOrElse { gitHubFailure ->
            gitHubFailure.addSuppressed(mirrorFailure)
            throw gitHubFailure
        }
    }

    private fun report(admission: Admission, outcome: Outcome, reason: String) {
        val pauseMillis = coolDown.report(admission, outcome) ?: return

        logger.log("$LOG_TAG Mirror skipped for $pauseMillis ms after $reason")
    }

    private fun logFallback(request: Request, reason: String) {
        logger.log("$LOG_TAG GitHub fallback for ${request.url.encodedPath}: $reason")
    }
}
