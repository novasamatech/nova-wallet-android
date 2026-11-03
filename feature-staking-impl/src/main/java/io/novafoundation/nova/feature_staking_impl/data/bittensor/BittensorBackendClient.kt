package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.data.cache.CachedResponse
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Response

class BittensorCacheStaleException : IllegalStateException("Bittensor snapshot is outdated")

/** Shared cache, origin and response-generation policy used only by typed Bittensor sources. */
class BittensorBackendClient(
    private val responses: ResponseCache,
    private val urls: InfrastructureUrls
) {

    private class Context(val base: HttpUrl, @Volatile var generation: BittensorRecommendationGeneration?)

    private val mutex = Mutex()

    @Volatile private var activeContext: Context? = null

    val foregroundRefreshes: Flow<Unit> get() = responses.foregroundRefreshes

    internal suspend fun url(path: String): String = bittensorBackendUrl(context().base, path)

    internal suspend fun <T> peek(url: String, type: Class<T>, metaAccountId: Long? = null, validate: (T) -> Unit = {}): T? =
        peekWithLifetime(url, type, metaAccountId, validate)?.value

    internal suspend fun <T> get(
        url: String,
        type: Class<T>,
        metaAccountId: Long? = null,
        allowStale: Boolean = false,
        validate: (T) -> Unit = {},
        generation: (T) -> BittensorRecommendationGeneration? = { null },
        fetch: suspend (String) -> Response<T>
    ): T = getWithLifetime(url, type, metaAccountId, allowStale, validate, generation, fetch).value

    // Paginated history needs the earliest page deadline to bound reuse of the assembled history.
    internal suspend fun <T> peekWithLifetime(url: String, type: Class<T>, metaAccountId: Long? = null, validate: (T) -> Unit = {}): CachedResponse<T>? {
        val context = context()
        requireUrl(context, url)
        requireOwner(url, metaAccountId)
        return responses.peek(url.toHttpUrl(), type, metaAccountId, validate)
    }

    internal suspend fun <T> getWithLifetime(
        url: String,
        type: Class<T>,
        metaAccountId: Long? = null,
        allowStale: Boolean = false,
        validate: (T) -> Unit = {},
        generation: (T) -> BittensorRecommendationGeneration? = { null },
        fetch: suspend (String) -> Response<T>
    ): CachedResponse<T> {
        val context = context()
        requireUrl(context, url)
        requireOwner(url, metaAccountId)
        val validateSnapshot: (T) -> Unit = { value ->
            validate(value)
            val order = generation(value)
            val newest = context.generation
            require(order == null || newest == null || order >= newest) { "Recommendation generation is stale" }
        }
        val result = responses.get(
            url = url.toHttpUrl(),
            type = type,
            metaAccountId = metaAccountId,
            allowStale = allowStale,
            validate = validateSnapshot,
            fetch = { endpoint ->
                val response = fetchInternal(context, endpoint, fetch)
                val order = response.body()?.let(generation)
                if (response.isSuccessful && order != null) {
                    validate(requireNotNull(response.body()))
                    admitGeneration(context, order, endpoint)
                }
                response
            }
        )
        checkCurrent(context)
        return result
    }

    internal fun <T> observe(
        url: suspend () -> String,
        type: Class<T>,
        metaAccountId: Long? = null,
        validate: (T) -> Unit = {},
        fetch: suspend (String) -> Response<T>
    ): Flow<T> = flow {
        val context = context()
        val endpoint = url()
        requireUrl(context, endpoint)
        requireOwner(endpoint, metaAccountId)
        emitAll(
            responses.observe(
                url = endpoint.toHttpUrl(),
                type = type,
                metaAccountId = metaAccountId,
                validate = validate,
                fetch = { fetchInternal(context, it, fetch) }
            ).map { response ->
                checkCurrent(context)
                response.value
            }
        )
    }

    internal suspend fun baseUrl(): HttpUrl = context().base

    /** Invalidates every query variant of one account endpoint and rejects its older in-flight writes. */
    internal suspend fun invalidateAccountResponses(base: HttpUrl, address: String, metaAccountId: Long, path: String) {
        val endpointPath = bittensorBackendUrl(base, path).toHttpUrl().encodedPath
        responses.invalidate { key ->
            if (key.metaAccountId != metaAccountId) return@invalidate false
            val url = key.normalizedUrl.toHttpUrlOrNull() ?: return@invalidate false
            if (!sameBackend(url, base)) return@invalidate false
            if (url.queryParameter("accountSubject") != address) return@invalidate false
            url.encodedPath == endpointPath
        }
    }

    private suspend fun context(): Context {
        val base = urls.baseUrl() ?: throw InfrastructureUrlMissingException()
        require(base.isHttps) { "Infrastructure requires HTTPS" }
        return mutex.withLock {
            val previous = activeContext
            if (previous?.base == base) return@withLock previous
            responses.invalidate { key ->
                val url = key.normalizedUrl.toHttpUrlOrNull()
                if (url == null) {
                    return@invalidate key.normalizedUrl.startsWith("bittensor://generation/") &&
                        key.normalizedUrl != bittensorGenerationKey(base)
                }
                url.encodedPath.startsWith("/v1/bittensor/") && (previous != null || !sameBackend(url, base))
            }
            val generation = responses.readSnapshot(
                bittensorGenerationKey(base),
                BittensorRecommendationGeneration::class.java,
                metaAccountId = null
            )
            Context(base, generation).also { activeContext = it }
        }
    }

    private suspend fun checkCurrent(context: Context) {
        check(urls.baseUrl() == context.base && activeContext === context) { "Bittensor origin has changed" }
    }

    private fun requireUrl(context: Context, url: String) {
        require(sameBackend(url.toHttpUrl(), context.base)) { "Bittensor origin has changed" }
    }

    private fun requireOwner(url: String, metaAccountId: Long?) {
        if ("accountSubject" in url.toHttpUrl().queryParameterNames) {
            requireNotNull(metaAccountId) { "Account responses require an explicit meta-account owner" }
        }
    }

    private fun sameBackend(url: HttpUrl, base: HttpUrl) = url.scheme == base.scheme && url.host == base.host && url.port == base.port

    private suspend fun <T> fetchInternal(context: Context, url: String, fetch: suspend (String) -> Response<T>): Response<T> {
        val response = try {
            withTimeout(BITTENSOR_REQUEST_TIMEOUT) {
                fetch(url)
            }
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            val path = url.toHttpUrl().encodedPath.removePrefix("/v1/bittensor/")
            throw BittensorTimeoutException("Backend $path", e)
        }
        checkCurrent(context)
        return response
    }

    private suspend fun admitGeneration(context: Context, generation: BittensorRecommendationGeneration, url: String) = mutex.withLock {
        check(activeContext === context) { "Bittensor origin has changed" }
        val newest = context.generation
        require(newest == null || generation >= newest) { "Recommendation generation is stale" }
        if (newest != null && generation > newest) {
            responses.invalidate { key ->
                val candidate = key.normalizedUrl.toHttpUrlOrNull() ?: return@invalidate false
                sameBackend(candidate, context.base) && candidate.toString() != url &&
                    candidate.encodedPath.startsWith("/v1/bittensor/recommendations")
            }
        }
        responses.saveSnapshot(bittensorGenerationKey(context.base), metaAccountId = null, value = generation, expiresAt = Long.MAX_VALUE)
        context.generation = generation
    }
}
