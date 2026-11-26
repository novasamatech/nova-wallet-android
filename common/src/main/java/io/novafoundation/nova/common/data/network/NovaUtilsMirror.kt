package io.novafoundation.nova.common.data.network

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * Reads nova-utils `master` files from the nova-infra config mirror and falls back to GitHub per request.
 *
 * Callers keep requesting the raw GitHub URL: the `*_URL` build fields and the URLs inside the configs stay
 * GitHub URLs, which are the fallback. A client reads the mirror only after it went through [addTo]; any other
 * client keeps reading GitHub. The policy is the same on iOS:
 *
 * - **Mapping.** A GET of `https://raw.githubusercontent.com/novasamatech/nova-utils/master/<path>` (or
 *   `refs/heads/master`) is first sent to `<mirror>/<path>`, without query and fragment. A path with a dot
 *   segment and every other request go to GitHub untouched.
 * - **Attempt.** One deadline for the whole attempt, body included: [maxAttemptMillis], and at most half of the
 *   caller's timeout. A redirect is not followed.
 * - **Accepted answer.** Status 200 with a non-empty `X-Config-Commit` header, which the mirror service puts on
 *   every 200, and a body of at most [maxBodyBytes], which for a `.json` file parses as JSON. Anything else is
 *   read from GitHub for that request; a mirror 404 is not final, as the file may be newer than the mirror's copy.
 * - **Pause.** A mirror-wide failure pauses the mirror for all clients: status 500 and above or 429, and a
 *   network error, a TLS error or the deadline when GitHub then answers the same request, which tells them from
 *   the device being offline. A 404 or another 4xx, a 200 that is not accepted, a redirect and a cancelled call
 *   do not pause it. How long and how it ends: [NovaUtilsMirrorCoolDown]. Cache-only requests are never skipped.
 * - **Trust.** The mirror is trusted as GitHub is. Its answer gets no integrity or freshness check: only the
 *   presence of `X-Config-Commit` is checked, its value is not read or compared. This is an accepted decision,
 *   not an omission.
 *
 * The mirror address is a build constant and not `infraUrl` of the global config (see `InfrastructureUrls`),
 * because that config is itself a nova-utils file read through the mirror. Both must name the same host;
 * `RealGlobalConfigDataSource` logs when they do not.
 */
class NovaUtilsMirror internal constructor(
    private val mirrorBaseUrl: HttpUrl,
    private val maxAttemptMillis: Long,
    private val maxBodyBytes: Long,
    private val coolDown: NovaUtilsMirrorCoolDown,
    private val logger: Logger
) {

    /**
     * [client] reading nova-utils files from the mirror first. Apply it last, to a finished client: the mirror
     * attempt is a call on [client] itself, so it uses the cache and the interceptors [client] has, and whatever
     * rewrites a request or its timeouts ([TimeHeaderInterceptor]) has to be in [client] already.
     */
    fun addTo(client: OkHttpClient): OkHttpClient {
        val mirrorClient = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        val interceptor = NovaUtilsMirrorInterceptor(
            mirrorBaseUrl = mirrorBaseUrl,
            mirrorClient = mirrorClient,
            maxAttemptMillis = maxAttemptMillis,
            maxBodyBytes = maxBodyBytes,
            coolDown = coolDown,
            logger = logger
        )

        return client.newBuilder()
            .addInterceptor(interceptor)
            .build()
    }
}
