package io.novafoundation.nova.feature_staking_impl.data.cache

import com.google.gson.Gson
import java.util.concurrent.atomic.AtomicBoolean
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.core_db.dao.ResponseCacheDao
import io.novafoundation.nova.core_db.model.ResponseCacheKey
import io.novafoundation.nova.core_db.model.ResponseCacheLocal
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response

data class CachedResponse<T>(val value: T, val fetchedAt: Long, val expiresAt: Long, val stale: Boolean, val freshUntilNanos: Long)

/** Canonicalize query names, retaining repeated values in their original order. Fragments are not sent over HTTP. */
fun normalizedResponseUrl(url: HttpUrl): String {
    require(url.username.isEmpty() && url.password.isEmpty()) { "Credentials cannot be cached in URLs" }
    val builder = url.newBuilder().fragment(null).query(null)
    url.queryParameterNames.sorted().forEach { name ->
        url.queryParameterValues(name).forEach { value -> builder.addQueryParameter(name, value) }
    }
    return builder.build().toString()
}

/** One durable store for HTTP responses and local snapshots, isolated by explicit wallet ownership. */
class ResponseCache(
    private val dao: ResponseCacheDao,
    private val gson: Gson,
    private val scope: CoroutineScope,
    private val logger: Logger,
    foreground: Flow<Boolean>,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val nanoTime: () -> Long = System::nanoTime
) {

    internal class SnapshotTicket(val key: ResponseCacheKey) {
        internal var invalidated = false
    }

    private class RequestInvalidated : IllegalStateException("Response request was invalidated")

    private class EntryState(var deadline: Long, var lastAttempt: Long? = null)

    private val mutex = Mutex()

    // A failed delete leaves disk contents untrustworthy. Bypass durable reads for this instance thereafter.
    private var durableReadsAvailable = true

    private class Flight {
        lateinit var result: Deferred<CachedResponse<*>>

        var readers = 0

        var completed = false

        var invalidated = false
    }

    private val flights = mutableMapOf<ResponseCacheKey, Flight>()

    private val states = linkedMapOf<ResponseCacheKey, EntryState>()

    // Only active operations retain tickets. TTL bookkeeping eviction must not invalidate their results.
    private val snapshotTickets = mutableMapOf<ResponseCacheKey, MutableSet<SnapshotTicket>>()

    private class Observer {
        val wake = Channel<Boolean>(Channel.CONFLATED)

        val invalidated = AtomicBoolean(false)

        fun invalidate() {
            invalidated.set(true)
            wake.trySend(true)
        }
    }

    private val observers = mutableMapOf<ResponseCacheKey, MutableSet<Observer>>()

    val foregroundRefreshes: Flow<Unit> = foreground.drop(1).filter { it }.coerceToUnit()

    suspend fun <T> peek(
        url: HttpUrl,
        type: Class<T>,
        metaAccountId: Long? = null,
        validate: (T) -> Unit = {}
    ): CachedResponse<T>? {
        val key = ResponseCacheKey(normalizedResponseUrl(url), metaAccountId)
        return read(key, type, validate)
    }

    suspend fun <T> get(
        url: HttpUrl,
        type: Class<T>,
        metaAccountId: Long? = null,
        allowStale: Boolean = false,
        validate: (T) -> Unit = {},
        fetch: suspend (String) -> Response<T>
    ): CachedResponse<T> {
        val key = ResponseCacheKey(normalizedResponseUrl(url), metaAccountId)
        val cached = peek(url, type, metaAccountId, validate)
        if (cached != null && !cached.stale) return cached
        if (allowStale && cached != null) {
            start(key, type, validate, fetch, background = true)
            return cached
        }
        val flight = requireNotNull(start(key, type, validate, fetch, background = false))
        try {
            val response = flight.result.await()
            return mutex.withLock {
                if (flight.invalidated) throw RequestInvalidated()
                requireOwnerExists(key)
                @Suppress("UNCHECKED_CAST")
                (response as CachedResponse<T>).copy(stale = nanoTime() >= response.freshUntilNanos)
            }
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    flight.readers--
                    removeCompletedFlight(key, flight)
                }
            }
        }
    }

    fun <T> observe(
        url: HttpUrl,
        type: Class<T>,
        metaAccountId: Long? = null,
        validate: (T) -> Unit = {},
        fetch: suspend (String) -> Response<T>
    ): Flow<CachedResponse<T>> = channelFlow {
        val key = ResponseCacheKey(normalizedResponseUrl(url), metaAccountId)
        val observer = Observer()
        val foregroundJob = launch { foregroundRefreshes.collect { observer.wake.trySend(true) } }
        suspend fun mayPublish(): Boolean {
            requireOwnerExists(key)
            return !observer.invalidated.get()
        }

        var explicitRefresh = false
        var latest: CachedResponse<T>? = null
        try {
            mutex.withLock { observers.getOrPut(key) { mutableSetOf() }.add(observer) }
            while (true) {
                if (observer.invalidated.getAndSet(false)) latest = null
                val stored = peek(url, type, metaAccountId, validate)
                if (stored != null && (latest == null || stored.fetchedAt > requireNotNull(latest).fetchedAt)) latest = stored
                latest = latest?.let { it.copy(stale = nanoTime() >= it.freshUntilNanos) }
                if (!mayPublish()) continue
                latest?.let { send(it) }
                try {
                    val refreshAvailable = mutex.withLock { key in flights || !recentlyAttempted(key) }
                    val needsRefresh = latest == null || latest.stale && (explicitRefresh || refreshAvailable)
                    if (needsRefresh) {
                        latest = get(url, type, metaAccountId, validate = validate, fetch = fetch)
                        if (!mayPublish()) continue
                        send(latest)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e is RequestInvalidated || observer.invalidated.get()) {
                        latest = null
                        explicitRefresh = true
                        continue
                    }
                    logger.log("[ResponseCache] Refresh failed for ${url.encodedPath}")
                    logger.log(e)
                    if (latest == null) throw e
                    if (!mayPublish()) continue
                    latest = latest.copy(stale = true)
                    send(latest)
                }
                val remaining = requireNotNull(latest).freshUntilNanos - nanoTime()
                val delayMillis = if (remaining > 0) (remaining / NANOS_PER_MILLI).coerceAtLeast(1) else RETRY_MILLIS
                explicitRefresh = withTimeoutOrNull(delayMillis) { observer.wake.receive() } ?: false
            }
        } finally {
            foregroundJob.cancel()
            withContext(NonCancellable) {
                mutex.withLock {
                    observers[key]?.let { registered ->
                        registered.remove(observer)
                        if (registered.isEmpty()) observers.remove(key)
                    }
                }
            }
            observer.wake.close()
        }
    }

    /** Local reads do not resolve infrastructure configuration or perform any transport. */
    internal suspend fun <T> readSnapshot(
        key: String,
        type: Class<T>,
        metaAccountId: Long?,
        validate: (T) -> Unit = {}
    ): T? = read(ResponseCacheKey(key, metaAccountId), type, validate)?.value

    /** Room owns change delivery, including foreign-key removal when the wallet is deleted. */
    internal fun <T> observeSnapshot(
        key: String,
        type: Class<T>,
        metaAccountId: Long?,
        validate: (T) -> Unit = {}
    ): Flow<T?> = dao.observe(key, metaAccountId)
        .map { readSnapshot(key, type, metaAccountId, validate) }
        .catch { error ->
            if (error is CancellationException) throw error
            logger.log("[ResponseCache] Snapshot observation unavailable")
            logger.log(error)
            emit(null)
        }
        .distinctUntilChanged()

    internal suspend fun <T> withSnapshotTicket(
        key: String,
        metaAccountId: Long,
        block: suspend (SnapshotTicket) -> T
    ): T = withTicket(ResponseCacheKey(key, metaAccountId), block)

    internal suspend fun isSnapshotCurrent(ticket: SnapshotTicket): Boolean = mutex.withLock {
        !ticket.invalidated && ownerExists(ticket.key)
    }

    /** True means the current snapshot was accepted; disk persistence is best effort. */
    internal suspend fun <T> saveSnapshot(
        key: String,
        metaAccountId: Long?,
        value: T,
        ticket: SnapshotTicket? = null,
        expiresAt: Long = 0L
    ): Boolean {
        val identity = ResponseCacheKey(key, metaAccountId)
        val row = ResponseCacheLocal(key, metaAccountId, gson.toJson(value), nowMillis(), expiresAt)
        return mutex.withLock {
            if (ticket != null && (ticket.key != identity || ticket.invalidated)) return@withLock false
            persist(row)
            true
        }
    }

    /** Detach matching flights before deleting. Kept snapshots stay visible, but old tickets can no longer write. */
    suspend fun invalidate(keepStale: Boolean = false, matches: (ResponseCacheKey) -> Boolean) {
        val result = mutex.withLock {
            val knownKeys = (flights.keys + states.keys + observers.keys + snapshotTickets.keys).distinct().filter(matches)
            knownKeys.forEach { key ->
                flights.remove(key)?.invalidated = true
                states.remove(key)
                observers[key]?.forEach { it.invalidate() }
                snapshotTickets.remove(key)?.forEach { it.invalidated = true }
            }
            runCatching {
                if (!keepStale) {
                    val keys = (dao.keys() + knownKeys).distinct().filter(matches)
                    dao.delete(keys.map { it.storageKey })
                }
            }.onFailure {
                durableReadsAvailable = false
            }
        }
        result.getOrThrow()
    }

    private suspend fun storedRow(key: ResponseCacheKey): ResponseCacheLocal? =
        if (durableReadsAvailable) accessStorage { dao.get(key.normalizedUrl, key.metaAccountId) } else null

    private suspend fun <T> withTicket(key: ResponseCacheKey, block: suspend (SnapshotTicket) -> T): T {
        val ticket = SnapshotTicket(key)
        mutex.withLock { snapshotTickets.getOrPut(key) { mutableSetOf() }.add(ticket) }
        return try {
            block(ticket)
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    ticket.invalidated = true
                    snapshotTickets[key]?.let { tickets ->
                        tickets.remove(ticket)
                        if (tickets.isEmpty()) snapshotTickets.remove(key)
                    }
                }
            }
        }
    }

    private suspend fun <T> read(key: ResponseCacheKey, type: Class<T>, validate: (T) -> Unit): CachedResponse<T>? = withTicket(key) { ticket ->
        val row = mutex.withLock { storedRow(key) } ?: return@withTicket null
        val value = try {
            decodePayload(row, type, validate)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[ResponseCache] Removing an incompatible snapshot")
            logger.log(e)
            mutex.withLock {
                if (!ticket.invalidated && storedRow(key) == row) {
                    accessStorage { dao.delete(listOf(key.storageKey)) }
                    states.remove(key)
                }
            }
            return@withTicket null
        }
        mutex.withLock {
            if (ticket.invalidated || accessStorage { ownerExists(key) } != true) return@withLock null
            val remaining = (row.expiresAt - nowMillis()).coerceIn(0L, MAX_AGE_MILLIS)
            val rowDeadline = nanoTime() + remaining * NANOS_PER_MILLI
            val deadline = minOf(state(key, rowDeadline).deadline, rowDeadline)
            val safeDeadline = if (nowMillis() < row.fetchedAt) nanoTime() else deadline
            CachedResponse(value, row.fetchedAt, row.expiresAt, nanoTime() >= safeDeadline, safeDeadline)
        }
    }

    private fun <T> decodePayload(
        row: ResponseCacheLocal,
        type: Class<T>,
        validate: (T) -> Unit
    ): T {
        val value = requireNotNull(gson.fromJson(row.payload, type))
        validate(value)
        return value
    }

    private suspend fun <T> start(
        key: ResponseCacheKey,
        type: Class<T>,
        validate: (T) -> Unit,
        fetch: suspend (String) -> Response<T>,
        background: Boolean
    ): Flight? = mutex.withLock {
        flights[key]?.let { flight ->
            if (!background) flight.readers++
            return@withLock flight
        }
        if (background && recentlyAttempted(key)) return@withLock null
        state(key).lastAttempt = nanoTime()
        val flight = Flight().apply { readers = if (background) 0 else 1 }
        flight.result = scope.async(start = CoroutineStart.LAZY) {
            try {
                // A previous flight may have completed between the caller's read and registration.
                val cached = read(key, type, validate)
                if (cached != null && !cached.stale) return@async cached
                val startedAt = nowMillis()
                val startedNanos = nanoTime()
                val response = fetch(key.normalizedUrl)
                if (!response.isSuccessful) throw HttpException(response)
                val body = requireNotNull(response.body()) { "Empty response" }
                validate(body)
                val lifetime = lifetimeMillis(response)
                val row = ResponseCacheLocal(key.normalizedUrl, key.metaAccountId, gson.toJson(body), startedAt, startedAt + lifetime)
                val deadline = startedNanos + lifetime * NANOS_PER_MILLI
                mutex.withLock {
                    currentCoroutineContext().ensureActive()
                    if (flight.invalidated) throw RequestInvalidated()
                    val saved = persist(row)
                    state(key).apply {
                        // A failed write must not grant the new response's lifetime to an older stored payload.
                        this.deadline = if (saved) deadline else nanoTime()
                        lastAttempt = startedNanos
                    }
                }
                CachedResponse(body, row.fetchedAt, row.expiresAt, nanoTime() >= deadline, deadline)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[ResponseCache] Request failed")
                logger.log(e)
                throw e
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        flight.completed = true
                        removeCompletedFlight(key, flight)
                    }
                }
            }
        }
        flights[key] = flight
        flight.result.start()
        flight
    }

    // Keep the invalidation token alive until every awaiting caller has checked it. Disk eviction is unrelated.
    private fun removeCompletedFlight(key: ResponseCacheKey, flight: Flight) {
        if (flight.completed && flight.readers == 0 && flights[key] === flight) flights.remove(key)
    }

    private suspend fun ownerExists(key: ResponseCacheKey): Boolean = key.metaAccountId?.let { dao.ownerExists(it) } ?: true

    private suspend fun requireOwnerExists(key: ResponseCacheKey) {
        check(ownerExists(key)) { "Response owner was deleted" }
    }

    private suspend fun persist(row: ResponseCacheLocal): Boolean {
        val key = ResponseCacheKey(row.normalizedUrl, row.metaAccountId)
        requireOwnerExists(key)
        val saved = accessStorage { dao.put(row); true } == true
        // An owner may have been deleted between the check and INSERT, which fails its foreign key.
        requireOwnerExists(key)
        if (saved) accessStorage { dao.trim(MAX_ENTRIES) }
        return saved
    }

    private suspend fun <T> accessStorage(action: suspend () -> T): T? = runCatching { action() }
        .onFailure { error ->
            if (error is CancellationException) throw error
            logger.log("[ResponseCache] Storage unavailable")
            logger.log(error)
        }
        .getOrNull()

    private fun recentlyAttempted(key: ResponseCacheKey): Boolean = states[key]?.lastAttempt?.let {
        nanoTime() - it < RETRY_MILLIS * NANOS_PER_MILLI
    } == true

    private fun state(key: ResponseCacheKey, deadline: Long = nanoTime()): EntryState {
        val state = states.remove(key) ?: EntryState(deadline)
        states[key] = state
        while (states.size > MAX_ENTRIES) states.remove(states.keys.first())
        return state
    }

    private fun lifetimeMillis(response: Response<*>): Long {
        val control = CacheControl.parse(response.headers())
        if (control.noStore || control.noCache) return 0
        val ageHeader = response.headers()["Age"]
        val age = if (ageHeader == null) 0L else ageHeader.toLongOrNull()?.coerceAtLeast(0) ?: return 0
        return (control.maxAgeSeconds.coerceIn(0, 300) - age).coerceAtLeast(0) * 1000L
    }

    private companion object {
        const val MAX_ENTRIES = 512
        const val MAX_AGE_MILLIS = 300_000L
        const val RETRY_MILLIS = 30_000L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
