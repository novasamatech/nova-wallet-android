package io.novafoundation.nova.common.data.network

import java.io.IOException
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.net.HttpURLConnection.HTTP_OK
import java.util.concurrent.TimeUnit
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
 * to the mirror; 200 and 304 are returned, and any other status or a network failure makes the original
 * GitHub request proceed as before. A mirror 404 is not final: the file may be newer than the mirror's copy.
 * All other requests pass through untouched.
 *
 * Must stay an application interceptor: it proceeds twice and adjusts timeouts. Only clients that register it
 * (the shared client and the image loader's client) read from the mirror; any other client keeps reading GitHub.
 *
 * @param mirrorAttemptTimeoutMillis upper bound for the connect and read timeouts of the mirror attempt,
 * so that an unreachable mirror delays the GitHub fallback less than an ordinary request timeout would.
 * @param standardTimeoutMillis the client's own timeout. A longer one was raised for the request on purpose
 * (see [TimeHeaderInterceptor]) and is kept for the mirror attempt.
 */
class NovaUtilsMirrorInterceptor(
    private val mirrorBaseUrl: HttpUrl,
    private val mirrorAttemptTimeoutMillis: Int,
    private val standardTimeoutMillis: Int
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val mirrorRequest = mirrorRequestFor(request) ?: return chain.proceed(request)

        return mirrorAnswer(chain, mirrorRequest) ?: chain.proceed(request)
    }

    private fun mirrorRequestFor(request: Request): Request? {
        if (request.method !in MIRRORED_METHODS) return null
        val mirrorUrl = novaUtilsMirrorUrl(request.url, mirrorBaseUrl) ?: return null

        return request.newBuilder().url(mirrorUrl).build()
    }

    /**
     * Returns the mirror's answer, or null when the file has to be read from GitHub.
     */
    private fun mirrorAnswer(chain: Interceptor.Chain, mirrorRequest: Request): Response? {
        val response = try {
            chain
                .withConnectTimeout(mirrorAttemptTimeout(chain.connectTimeoutMillis()), TimeUnit.MILLISECONDS)
                .withReadTimeout(mirrorAttemptTimeout(chain.readTimeoutMillis()), TimeUnit.MILLISECONDS)
                .proceed(mirrorRequest)
        } catch (mirrorFailure: IOException) {
            // A cancelled call fails here as well; it must not start another request.
            if (chain.call().isCanceled()) throw mirrorFailure
            return null
        }

        if (response.code in MIRROR_ANSWER_CODES) return response

        // OkHttp refuses to proceed again while the previous response is still open.
        response.close()
        return null
    }

    private fun mirrorAttemptTimeout(requestTimeoutMillis: Int): Int {
        val isRaisedForRequest = requestTimeoutMillis > standardTimeoutMillis

        return if (isRaisedForRequest) requestTimeoutMillis else min(requestTimeoutMillis, mirrorAttemptTimeoutMillis)
    }
}
