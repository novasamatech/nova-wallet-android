package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.BittensorCacheDao
import io.novafoundation.nova.core_db.model.BittensorCacheLocal
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.security.MessageDigest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response

/** HTTP lifetime and backend component freshness are separate; neither makes a stale snapshot live transaction data. */
data class BittensorCachedResponse<T>(
    val value: T,
    val fetchedAt: Long,
    val expiresAt: Long,
    val stale: Boolean,
    val freshUntilNanos: Long
)

class BittensorCacheStaleException : IllegalStateException("Bittensor snapshot is outdated")

/**
 * Feature-lifetime flights outlive a screen, but not their origin/account revision. The mutex protects admission and
 * persistence only; network requests run outside it. Persist validated DTOs, never HTTP credentials or raw responses.
 */
class BittensorResponseCache(
    private val dao: BittensorCacheDao,
    private val gson: Gson,
    private val urls: InfrastructureUrls,
    private val scope: CoroutineScope,
    private val foreground: Flow<Boolean>,
    private val logger: Logger,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val nanoTime: () -> Long = System::nanoTime
) {

    internal data class SnapshotTicket(val base: HttpUrl, val epoch: Long, val revision: Long)

    private data class Context(val base: HttpUrl, val namespace: String, val epoch: Long)

    private val mutex = Mutex()

    private val flights = mutableMapOf<String, Deferred<BittensorCacheLocal>>()

    private val revisions = mutableMapOf<String, Long>()

    private val deadlines = mutableMapOf<String, Long>()

    private val attempts = mutableMapOf<String, Long>()

    private data class Invalidation(val namespace: String, val account: String, val resources: List<String>)

    private val changes = MutableSharedFlow<Invalidation>(extraBufferCapacity = 64)

    // Shared across all callers, including simultaneous dashboard and picker yield bursts.
    private val yieldRequests = Semaphore(4)

    val foregroundRefreshes: Flow<Unit> = foreground.filter { it }.map { Unit }

    private var activeBase: HttpUrl? = null

    private var epoch = 0L

    suspend fun <T> peek(
        path: String,
        type: Class<T>,
        account: String = "",
        query: String = "",
        validate: (T) -> Unit = {}
    ): BittensorCachedResponse<T>? {
        val context = context()
        return read(context, key(context, path, account, query), type, validate)
    }

    internal suspend fun <T> get(
        path: String,
        type: Class<T>,
        account: String = "",
        query: String = "",
        allowStale: Boolean = false,
        force: Boolean = false,
        expectedBase: HttpUrl? = null,
        persist: Boolean = true,
        retryCooldown: Boolean = false,
        validate: (T) -> Unit = {},
        generation: (T) -> BittensorRecommendationGeneration? = { null },
        fetch: suspend (String) -> Response<T>
    ): BittensorCachedResponse<T> {
        val context = context()
        check(expectedBase == null || expectedBase == context.base) { "Bittensor origin has changed" }
        val key = key(context, path, account, query)
        val admittedRevision = mutex.withLock { revision(context, account) }
        val cached = read(context, key, type, validate)
        if (!force && cached != null && !cached.stale) return cached
        val recentlyAttempted = mutex.withLock {
            attempts[key]?.let { nanoTime() - it < MIN_REFRESH_MILLIS * NANOS_PER_MILLI } == true
        }
        if (!force && retryCooldown && cached != null && recentlyAttempted) return cached
        if (!force && allowStale && cached != null) {
            start(context, key, path, account, persist, validate, generation, fetch, bounded = true, reuseFresh = !force)
            return cached
        }
        val flight = start(context, key, path, account, persist, validate, generation, fetch, bounded = false, reuseFresh = !force)
        val row = requireNotNull(flight).await()
        checkCurrent(context)
        return mutex.withLock {
            checkContext(context)
            check(admittedRevision == revision(context, account)) { "Bittensor account cache was invalidated" }
            decode(row, type, validate)
        }
    }

    /** Cached first, then network, then expiry/foreground/invalidation driven refresh. Failures retain visible data. */
    fun <T> observe(
        path: String,
        type: Class<T>,
        account: String = "",
        query: String = "",
        validate: (T) -> Unit = {},
        fetch: suspend (String) -> Response<T>
    ): Flow<BittensorCachedResponse<T>> = channelFlow {
        val wake = Channel<Boolean>(Channel.CONFLATED)
        val signals = launch {
            coroutineScope {
                launch { foreground.drop(1).filter { it }.collect { wake.trySend(true) } }
                launch {
                    changes.filter { change ->
                        change.account == hash(account) && path in change.resources &&
                            mutex.withLock { activeBase?.toString()?.let(::hash) == change.namespace }
                    }.collect { wake.trySend(true) }
                }
            }
        }
        var bypassCooldown = false
        try {
            while (true) {
                var latest = peek(path, type, account, query, validate)
                latest?.let { send(it) }
                try {
                    latest = get(path, type, account, query, retryCooldown = !bypassCooldown, validate = validate, fetch = fetch)
                    send(latest)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.log("[BittensorCache] Refresh failed for $path")
                    logger.log(e)
                    if (latest == null) throw e
                    latest = latest.copy(stale = true)
                    send(latest)
                }
                val remaining = latest.expiresAt - nowMillis()
                val refreshDelay = if (remaining > 0) remaining.coerceAtMost(MAX_REFRESH_MILLIS) else MIN_REFRESH_MILLIS
                bypassCooldown = withTimeoutOrNull(refreshDelay) { wake.receive() } ?: false
            }
        } finally {
            signals.cancel()
            wake.close()
        }
    }

    /** A command, rather than a side effect of a read; old flights cannot repopulate invalidated account data. */
    suspend fun invalidate(base: HttpUrl, account: String, resources: List<String>, keepStale: Boolean = false) {
        val namespace = hash(base.toString())
        val accountKey = hash(account)
        mutex.withLock {
            revisions["$namespace|$accountKey"] = (revisions["$namespace|$accountKey"] ?: 0L) + 1L
            if (!keepStale) dao.invalidate(namespace, accountKey, resources)
            // Runtime snapshots always restore as stale, so retain their last known values for an offline restart.
            // Account revision still rejects writes by loads started before this transaction.
        }
        changes.tryEmit(Invalidation(namespace, accountKey, resources))
    }

    /** Runtime holdings use the same durable store, and always restore as stale until a live chain read succeeds. */
    internal suspend fun snapshotTicket(account: String): SnapshotTicket {
        val context = context()
        return mutex.withLock { SnapshotTicket(context.base, context.epoch, revision(context, account)) }
    }

    internal suspend fun <T> saveSnapshot(path: String, account: String, value: T, ticket: SnapshotTicket? = null): Boolean {
        val context = context()
        val key = key(context, path, account, "")
        return mutex.withLock {
            checkContext(context)
            if (ticket != null && (ticket.base != context.base || ticket.epoch != context.epoch || ticket.revision != revision(context, account))) {
                return@withLock false
            }
            dao.put(BittensorCacheLocal(key, context.namespace, path, hash(account), gson.toJson(value), nowMillis(), 0L, null, null))
            deadlines.remove(key)
            dao.trim(MAX_ENTRIES)
            true
        }
    }

    private suspend fun context(): Context {
        val base = urls.baseUrl() ?: throw InfrastructureUrlMissingException()
        require(base.isHttps) { "Infrastructure requires HTTPS" }
        return mutex.withLock {
            if (base != activeBase) {
                activeBase = base
                epoch += 1
                deadlines.clear()
                attempts.clear()
                // Persisted namespaces cannot revive after changing away and back, including across restarts.
                dao.keepNamespace(hash(base.toString()))
            }
            Context(base, hash(base.toString()), epoch)
        }
    }

    private suspend fun checkCurrent(context: Context) {
        check(urls.baseUrl() == context.base) { "Bittensor origin has changed" }
        mutex.withLock { checkContext(context) }
    }

    private fun checkContext(context: Context) {
        check(activeBase == context.base && epoch == context.epoch) { "Bittensor cache context has changed" }
    }

    private fun key(context: Context, path: String, account: String, query: String) = hash("v1|${context.namespace}|$path|$account|$query")

    private fun revision(context: Context, account: String) = revisions["${context.namespace}|${hash(account)}"] ?: 0L

    private suspend fun <T> read(context: Context, key: String, type: Class<T>, validate: (T) -> Unit): BittensorCachedResponse<T>? = mutex.withLock {
        checkContext(context)
        val row = dao.get(key) ?: return@withLock null
        try {
            decode(row, type, validate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorCache] Ignoring an invalid stored snapshot")
            logger.log(e)
            null
        }
    }

    private fun <T> decode(row: BittensorCacheLocal, type: Class<T>, validate: (T) -> Unit): BittensorCachedResponse<T> {
        val value = requireNotNull(gson.fromJson(row.payload, type))
        validate(value)
        // A clock moved backwards cannot turn old snapshots fresh for longer than the protocol cap.
        val remaining = (row.expiresAt - nowMillis()).coerceIn(0L, MAX_REFRESH_MILLIS)
        val deadline = if (nowMillis() < row.fetchedAt) {
            nanoTime().also { deadlines[row.key] = it }
        } else {
            deadlines.getOrPut(row.key) { nanoTime() + remaining * NANOS_PER_MILLI }
        }
        return BittensorCachedResponse(value, row.fetchedAt, row.expiresAt, nanoTime() >= deadline, deadline)
    }

    private suspend fun <T> start(
        context: Context,
        key: String,
        path: String,
        account: String,
        persist: Boolean,
        validate: (T) -> Unit,
        generation: (T) -> BittensorRecommendationGeneration?,
        fetch: suspend (String) -> Response<T>,
        bounded: Boolean,
        reuseFresh: Boolean
    ): Deferred<BittensorCacheLocal>? = mutex.withLock {
        checkContext(context)
        val revision = revision(context, account)
        val flightKey = "$key|${context.epoch}|$revision"
        flights[flightKey]?.let { return@withLock it }
        if (bounded && attempts[key]?.let { nanoTime() - it < MIN_REFRESH_MILLIS * NANOS_PER_MILLI } == true) return@withLock null
        val stored = dao.get(key)
        if (reuseFresh && !bounded && stored != null && deadlines[key]?.let { nanoTime() < it } == true) {
            return@withLock kotlinx.coroutines.CompletableDeferred(stored)
        }
        attempts[key] = nanoTime()
        val deferred = scope.async(start = CoroutineStart.LAZY) {
            try {
                val startedAt = nowMillis()
                val startedNanos = nanoTime()
                val url = bittensorBackendUrl(context.base, path)
                val response = try {
                    // Bound the entire flight, including its wait for a yield slot, after a screen is cancelled.
                    withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
                        if (path.endsWith("/yields/alpha")) yieldRequests.withPermit { fetch(url) } else fetch(url)
                    } ?: throw BittensorTimeoutException("Backend $path")
                } catch (e: TimeoutCancellationException) {
                    currentCoroutineContext().ensureActive()
                    throw BittensorTimeoutException("Backend $path", e)
                }
                if (!response.isSuccessful) throw HttpException(response)
                val value = requireNotNull(response.body()) { "Empty Bittensor response" }
                validate(value)
                val order = generation(value)
                val lifetime = bittensorCacheLifetimeNanos(response)
                val row = BittensorCacheLocal(
                    key, context.namespace, path, hash(account), gson.toJson(value), startedAt,
                    startedAt + lifetime / NANOS_PER_MILLI, order?.asOf?.toPlainString(), order?.sourceBlockNumber?.toString()
                )
                checkCurrent(context)
                mutex.withLock {
                    checkContext(context)
                    check(revision == revision(context, account)) { "Bittensor account cache was invalidated" }
                    if (order != null) admitGeneration(context, order)
                    if (persist) {
                        dao.put(row)
                        dao.trim(MAX_ENTRIES)
                    }
                    deadlines[key] = startedNanos + lifetime
                }
                row
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorCache] Request failed for $path")
                logger.log(e)
                throw e
            } finally {
                mutex.withLock { flights.remove(flightKey) }
            }
        }
        flights[flightKey] = deferred
        deferred.start()
        deferred
    }

    private suspend fun admitGeneration(context: Context, generation: BittensorRecommendationGeneration) {
        val newest = dao.generations(context.namespace).map {
            BittensorRecommendationGeneration(requireNotNull(it.generationAsOf).toBigDecimal(), requireNotNull(it.generationBlock).toBigInteger())
        }.maxOrNull()
        require(newest == null || generation >= newest) { "Recommendation generation is stale" }
        if (newest != null && generation > newest) dao.dropGenerations(context.namespace)
        dao.put(
            BittensorCacheLocal(
                hash("${context.namespace}|generation"), context.namespace, "__generation", hash(""), "{}", nowMillis(), 0L,
                generation.asOf.toPlainString(), generation.sourceBlockNumber.toString()
            )
        )
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MIN_REFRESH_MILLIS = 30_000L
        const val MAX_REFRESH_MILLIS = 300_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAX_ENTRIES = 512
    }
}
