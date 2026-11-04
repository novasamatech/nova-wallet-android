package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ResponseCacheDao
import io.novafoundation.nova.core_db.model.ResponseCacheLocal
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import io.novafoundation.nova.feature_staking_impl.data.cache.CachedResponse
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.Mockito.mock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ResponseCacheTest {

    private data class Payload(val value: String)

    private var base = "https://infra.test/".toHttpUrl()

    private fun endpoint(path: String) = bittensorBackendUrl(base, path).toHttpUrl()

    private val dao = InMemoryResponseCacheDao()

    private var wall = 1_000_000L

    private var nanos = 0L

    private val wallClock = { wall }

    private val monotonicClock = { nanos }

    private fun response(value: String, cacheControl: String = "max-age=60", age: String = "0") =
        Response.success(Payload(value), Headers.headersOf("Cache-Control", cacheControl, "Age", age))

    @Test
    fun `active snapshot ticket survives unrelated TTL state eviction`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val key = BittensorCacheKeys.positions("chain", "owner")
        cache.withSnapshotTicket(key, 1L) { ticket ->
            repeat(513) { index ->
                wall++
                cache.get(endpoint("other/$index"), Payload::class.java) { response("other") }
            }
            assertTrue(cache.isSnapshotCurrent(ticket))
            assertTrue(cache.saveSnapshot(key, 1L, Payload("positions"), ticket))
        }
        assertEquals("positions", cache.readSnapshot(key, Payload::class.java, 1L)?.value)
    }

    @Test
    fun `snapshot ticket ends with its operation on completion cancellation and owner deletion`() = runTest {
        val owners = mutableSetOf(1L)
        val ownerDao = InMemoryResponseCacheDao(owners)
        val cache = createResponseTestCache(ownerDao, backgroundScope, wallClock, monotonicClock)
        val key = BittensorCacheKeys.positions("chain", "owner")
        val completed = cache.withSnapshotTicket(key, 1L) { it }
        assertFalse(cache.isSnapshotCurrent(completed))
        assertFalse(cache.saveSnapshot(key, 1L, Payload("late"), completed))

        val captured = CompletableDeferred<ResponseCache.SnapshotTicket>()
        val operation = launch {
            cache.withSnapshotTicket(key, 1L) { ticket ->
                captured.complete(ticket)
                awaitCancellation()
            }
        }
        val cancelled = captured.await()
        operation.cancelAndJoin()
        assertFalse(cache.isSnapshotCurrent(cancelled))

        cache.withSnapshotTicket(key, 1L) { ticket ->
            assertTrue(cache.isSnapshotCurrent(ticket))
            ownerDao.deleteOwner(1L)
            assertFalse(cache.isSnapshotCurrent(ticket))
            assertTrue(runCatching { cache.saveSnapshot(key, 1L, Payload("late"), ticket) }.isFailure)
        }
    }

    @Test
    fun `fresh observation emits once and does not fetch or decode a second time`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("subnets"), Payload::class.java) { response("fresh") }
        val updates = mutableListOf<CachedResponse<Payload>>()
        var validations = 0
        val observer = backgroundScope.launch {
            cache.observe(
                url = endpoint("subnets"),
                type = Payload::class.java,
                validate = { validations++ },
                fetch = { error("Fresh data must not fetch") }
            ).toList(updates)
        }
        runCurrent()
        assertEquals(listOf("fresh"), updates.map { it.value.value })
        assertEquals(1, validations)
        observer.cancelAndJoin()
    }

    @Test
    fun `successful request returns its validated body without decoding serialized JSON`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val body = Payload("live")
        var validations = 0
        val result = cache.get(
            url = endpoint("subnets"),
            type = Payload::class.java,
            validate = { validations++ },
            fetch = { Response.success(body) }
        )
        assertSame(body, result.value)
        assertEquals(1, validations)
    }

    @Test
    fun `snapshot validation runs outside registry lock and invalidation rejects its decoded result`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val key = BittensorCacheKeys.positions("chain", "owner")
        cache.saveSnapshot(key, 1L, Payload("old"))

        val result = cache.readSnapshot(key, Payload::class.java, 1L) {
            runBlocking {
                withTimeout(1_000) { cache.invalidate { it.normalizedUrl == key } }
            }
        }

        assertNull(result)
    }

    @Test
    fun `snapshot observation follows save invalidation and owner cascade without polling`() = runTest {
        val owners = mutableSetOf(1L, 2L)
        val ownerDao = InMemoryResponseCacheDao(owners)
        val cache = createResponseTestCache(ownerDao, backgroundScope, wallClock, monotonicClock)
        val key = BittensorCacheKeys.positions("chain", "owner")
        val updates = mutableListOf<Payload?>()
        val observer = backgroundScope.launch { cache.observeSnapshot(key, Payload::class.java, 1L).toList(updates) }
        runCurrent()
        cache.saveSnapshot(key, 2L, Payload("another wallet"))
        cache.saveSnapshot(key, 1L, Payload("first"))
        runCurrent()
        cache.invalidate { it.metaAccountId == 1L }
        runCurrent()
        cache.saveSnapshot(key, 1L, Payload("second"))
        runCurrent()
        ownerDao.deleteOwner(1L)
        runCurrent()

        assertEquals(listOf(null, Payload("first"), null, Payload("second"), null), updates)
        observer.cancelAndJoin()
    }

    @Test
    fun `cancelled invalidation still prevents fallback to removed data when replacement fails`() = runTest {
        val cancelledDao = object : ResponseCacheDao by dao {
            override suspend fun keys(): List<io.novafoundation.nova.core_db.model.ResponseCacheKey> =
                throw CancellationException("Invalidation cancelled")
        }
        val cache = createResponseTestCache(cancelledDao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("subnets"), Payload::class.java) { response("old", "no-store") }
        nanos += 30_000_000_000L
        wall += 30_000
        val release = CompletableDeferred<Unit>()
        val updates = mutableListOf<CachedResponse<Payload>>()
        var requests = 0
        val observer = async {
            runCatching {
                cache.observe(endpoint("subnets"), Payload::class.java) {
                    if (requests++ > 0) throw IOException("Replacement failed")
                    release.await()
                    response("obsolete flight")
                }.toList(updates)
            }
        }
        runCurrent()
        assertEquals(listOf("old"), updates.map { it.value.value })
        assertTrue(runCatching { cache.invalidate { true } }.exceptionOrNull() is CancellationException)
        release.complete(Unit)
        runCurrent()

        assertTrue(observer.await().isFailure)
        assertEquals(2, requests)
        assertEquals(listOf("old"), updates.map { it.value.value })
    }

    @Test
    fun `invalidation during refresh never republishes the removed response`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("subnets"), Payload::class.java) { response("old", "no-store") }
        nanos += 30_000_000_000L
        wall += 30_000
        val release = CompletableDeferred<Unit>()
        val updates = mutableListOf<CachedResponse<Payload>>()
        var requests = 0
        val observer = backgroundScope.launch {
            cache.observe(endpoint("subnets"), Payload::class.java) {
                if (requests++ == 0) {
                    release.await()
                    response("obsolete flight")
                } else {
                    response("replacement")
                }
            }.toList(updates)
        }
        runCurrent()
        assertEquals(listOf("old"), updates.map { it.value.value })
        cache.invalidate { true }
        val beforeInvalidation = updates.size
        release.complete(Unit)
        runCurrent()

        assertTrue(updates.drop(beforeInvalidation).isNotEmpty())
        assertTrue(updates.drop(beforeInvalidation).all { it.value.value == "replacement" })
        observer.cancel()
    }

    @Test
    fun `deleted owner cannot keep receiving the previous response through refresh fallback`() = runTest {
        val owners = mutableSetOf(1L)
        val ownerDao = InMemoryResponseCacheDao(owners)
        val cache = createResponseTestCache(ownerDao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("positions"), Payload::class.java, metaAccountId = 1L) { response("private", "no-store") }
        nanos += 30_000_000_000L
        wall += 30_000
        val release = CompletableDeferred<Unit>()
        val updates = mutableListOf<CachedResponse<Payload>>()
        val observer = async {
            runCatching {
                cache.observe(endpoint("positions"), Payload::class.java, metaAccountId = 1L) {
                    release.await()
                    response("late private")
                }.toList(updates)
            }
        }
        runCurrent()
        assertEquals(listOf("private"), updates.map { it.value.value })
        ownerDao.deleteOwner(1L)
        release.complete(Unit)
        runCurrent()

        assertTrue(observer.await().isFailure)
        assertEquals(listOf("private"), updates.map { it.value.value })
    }

    @Test
    fun `observer retains last successful data when persistence and later refresh both fail`() = runTest {
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun put(entry: ResponseCacheLocal) = throw IOException("Disk full")
        }
        val cache = createResponseTestCache(failingDao, backgroundScope, wallClock, monotonicClock)
        val updates = mutableListOf<CachedResponse<Payload>>()
        var requests = 0
        val observer = launch {
            cache.observe(endpoint("subnets"), Payload::class.java) {
                if (requests++ > 0) throw IOException("Offline")
                response("live", "no-store")
            }.take(3).toList(updates)
        }
        runCurrent()
        assertEquals("live", updates.single().value.value)
        nanos += 30_000_000_000L
        wall += 30_000
        advanceTimeBy(30_000)
        runCurrent()
        observer.join()

        assertEquals(2, requests)
        assertTrue(updates.all { it.value.value == "live" })
        assertTrue(updates.last().stale)
    }

    @Test
    fun `failed invalidation rejects in-flight results and disables stale durable reads`() = runTest {
        var failInvalidation = false
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun keys() = if (failInvalidation) throw IOException("Cannot list keys") else dao.keys()
        }
        val cache = createResponseTestCache(failingDao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("saved"), Payload::class.java) { response("obsolete saved") }
        val release = CompletableDeferred<Unit>()
        val pending = async { runCatching { cache.get(endpoint("pending"), Payload::class.java) { release.await(); response("obsolete flight") } } }
        runCurrent()
        failInvalidation = true

        assertTrue(runCatching { cache.invalidate { true } }.isFailure)
        release.complete(Unit)

        assertTrue(pending.await().isFailure)
        assertNull(cache.peek(endpoint("saved"), Payload::class.java))
        assertEquals("replacement", cache.get(endpoint("saved"), Payload::class.java) { response("replacement") }.value.value)
    }

    @Test
    fun `failed replacement does not extend the older stored response lifetime`() = runTest {
        var failWrites = false
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun put(entry: ResponseCacheLocal) {
                if (failWrites) throw IOException("Disk full")
                dao.put(entry)
            }
        }
        val cache = createResponseTestCache(failingDao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("subnets"), Payload::class.java) { response("old") }
        wall += 61_000
        nanos += 61_000_000_000L
        failWrites = true

        val live = cache.get(endpoint("subnets"), Payload::class.java) { response("live") }
        val stored = requireNotNull(cache.peek(endpoint("subnets"), Payload::class.java))

        assertEquals("live", live.value.value)
        assertFalse(live.stale)
        assertEquals("old", stored.value.value)
        assertTrue(stored.stale)
    }

    @Test
    fun `successful response is delivered when cache reads and writes fail`() = runTest {
        val unavailableDao = object : ResponseCacheDao by dao {
            override suspend fun get(url: String, metaAccountId: Long?): ResponseCacheLocal? = throw IOException("Cache unreadable")
            override suspend fun put(entry: ResponseCacheLocal) = throw IOException("Disk full")
        }
        val cache = createResponseTestCache(unavailableDao, backgroundScope, wallClock, monotonicClock)

        val result = cache.get(endpoint("subnets"), Payload::class.java) { response("live") }

        assertEquals("live", result.value.value)
        assertFalse(result.stale)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `failed wallet ownership lookup treats a stored snapshot as unavailable`() = runTest {
        val key = BittensorCacheKeys.positions("chain", "owner")
        dao.put(ResponseCacheLocal(key, 1L, "{\"value\":\"private\"}", wall, 0))
        val unavailableDao = object : ResponseCacheDao by dao {
            override suspend fun ownerExists(metaAccountId: Long): Boolean = throw IOException("Cache unavailable")
        }
        val cache = createResponseTestCache(unavailableDao, backgroundScope, wallClock, monotonicClock)

        assertNull(cache.readSnapshot(key, Payload::class.java, 1L))
    }

    @Test
    fun `trim failure does not discard accepted response or snapshot`() = runTest {
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun trim(limit: Int) = throw IOException("Cannot trim")
        }
        val cache = createResponseTestCache(failingDao, backgroundScope, wallClock, monotonicClock)
        val key = BittensorCacheKeys.positions("chain", "owner")
        assertEquals("live", cache.get(endpoint("subnets"), Payload::class.java) { response("live") }.value.value)
        cache.withSnapshotTicket(key, 1L) { ticket ->
            assertTrue(cache.saveSnapshot(key, 1L, Payload("positions"), ticket))
            assertTrue(cache.isSnapshotCurrent(ticket))
        }
    }

    @Test
    fun `snapshot remains accepted on disk failure but invalidation still rejects its ticket`() = runTest {
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun put(entry: ResponseCacheLocal) = throw IOException("Disk full")
        }
        val cache = createResponseTestCache(failingDao, backgroundScope, wallClock, monotonicClock)
        val key = BittensorCacheKeys.positions("chain", "owner")
        cache.withSnapshotTicket(key, 1L) { ticket ->
            assertTrue(cache.saveSnapshot(key, 1L, Payload("positions"), ticket))
            assertTrue(cache.isSnapshotCurrent(ticket))
            cache.invalidate(keepStale = true) { it.metaAccountId == 1L }
            assertFalse(cache.isSnapshotCurrent(ticket))
            assertFalse(cache.saveSnapshot(key, 1L, Payload("obsolete"), ticket))
        }
    }

    @Test
    fun `cache eviction does not invalidate a successful slow response`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val release = CompletableDeferred<Unit>()
        val slow = async { cache.get(endpoint("slow"), Payload::class.java) { release.await(); response("slow result") } }
        runCurrent()
        repeat(513) { index ->
            wall++
            cache.get(endpoint("new/$index"), Payload::class.java) { response("new result") }
        }
        release.complete(Unit)

        assertEquals("slow result", slow.await().value.value)
        assertNull(cache.peek(endpoint("slow"), Payload::class.java))
        assertEquals(512, dao.rows.size)
    }

    @Test
    fun `intentional invalidation rejects response even when its storage would fail`() = runTest {
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun put(entry: ResponseCacheLocal) = throw IOException("Disk full")
        }
        val cache = createResponseTestCache(failingDao, backgroundScope, wallClock, monotonicClock)
        val release = CompletableDeferred<Unit>()
        val pending = async { runCatching { cache.get(endpoint("subnets"), Payload::class.java) { release.await(); response("obsolete") } } }
        runCurrent()
        cache.invalidate { true }
        release.complete(Unit)

        assertTrue(pending.await().isFailure)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `deleting owner after persistence prevents delivery without relying on stored row equality`() = runTest {
        val owners = mutableSetOf(1L)
        val ownerDao = InMemoryResponseCacheDao(owners)
        val deletingDao = object : ResponseCacheDao by ownerDao {
            override suspend fun trim(limit: Int) {
                ownerDao.deleteOwner(1L)
            }
        }
        val cache = createResponseTestCache(deletingDao, backgroundScope, wallClock, monotonicClock)

        val result = runCatching { cache.get(endpoint("positions"), Payload::class.java, metaAccountId = 1L) { response("private") } }

        assertTrue(result.isFailure)
    }

    @Test
    fun `cancelling a reader preserves shared request and storage cancellation is not swallowed`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val first = async { cache.get(endpoint("subnets"), Payload::class.java) { calls++; release.await(); response("live") } }
        runCurrent()
        val second = async { cache.get(endpoint("subnets"), Payload::class.java) { error("Duplicated request") } }
        runCurrent()
        first.cancel()
        release.complete(Unit)
        assertEquals("live", second.await().value.value)
        assertEquals(1, calls)

        val cancelledDao = object : ResponseCacheDao by dao {
            override suspend fun put(entry: ResponseCacheLocal): Unit = throw CancellationException("Storage cancelled")
        }
        val cancelledCache = createResponseTestCache(cancelledDao, backgroundScope, wallClock, monotonicClock)
        val result = runCatching { cancelledCache.get(endpoint("cancelled"), Payload::class.java) { response("live") } }
        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `first real foreground transition is retained when observation starts in background`() = runTest {
        val foreground = MutableStateFlow(false)
        val gson = Gson()
        val logger = mock(Logger::class.java)
        val cache = ResponseCache(dao, gson, backgroundScope, logger, foreground, wallClock, monotonicClock)
        val event = async { cache.foregroundRefreshes.first() }
        runCurrent()
        assertFalse(event.isCompleted)

        foreground.value = true
        runCurrent()

        assertTrue(event.isCompleted)
    }

    @Test
    fun `expiry subtracts Age caps at five minutes and starts before transport`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val result = cache.get(endpoint("subnets"), Payload::class.java) {
            wall += 20_000
            nanos += 20_000_000_000L
            response("saved", "public, max-age=600", "30")
        }
        assertEquals(1_000_000L, result.fetchedAt)
        assertEquals(1_270_000L, result.expiresAt)
        wall = 1_270_000L
        nanos = 270_000_000_000L
        assertTrue(cache.peek(endpoint("subnets"), Payload::class.java)!!.stale)
    }

    @Test
    fun `no store and no cache remain displayable without extending freshness`() = runTest {
        for (control in listOf("no-store", "no-cache, max-age=270")) {
            val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
            val result = cache.get(endpoint(control), Payload::class.java) { response("stale-first", control) }
            assertTrue(result.stale)
            assertEquals(result.fetchedAt, result.expiresAt)
            val restarted = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
            assertEquals("stale-first", restarted.peek(endpoint(control), Payload::class.java)!!.value.value)
            assertTrue(restarted.peek(endpoint(control), Payload::class.java)!!.stale)
        }
    }

    @Test
    fun `identical concurrent requests share one flight while independent endpoints finish`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val first = async { cache.get(endpoint("validators"), Payload::class.java) { calls++; release.await(); response("directory") } }
        runCurrent()
        val second = async { cache.get(endpoint("validators"), Payload::class.java) { calls++; response("duplicate") } }
        val recommendations = async { cache.get(endpoint("recommendations"), Payload::class.java) { response("recommendations") } }
        val root = async { cache.get(endpoint("root/validators"), Payload::class.java) { response("root") } }
        runCurrent()
        assertEquals("recommendations", recommendations.await().value.value)
        assertEquals("root", root.await().value.value)
        assertFalse(first.isCompleted)
        release.complete(Unit)
        assertEquals("directory", first.await().value.value)
        assertEquals("directory", second.await().value.value)
        assertEquals(1, calls)
    }

    @Test
    fun `expired payload emits immediately then refreshed payload follows`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("subnets"), Payload::class.java) { response("old", "no-store") }
        wall += 30_000
        nanos += 30_000_000_000L
        val release = CompletableDeferred<Unit>()
        val seen = mutableListOf<CachedResponse<Payload>>()
        val collect = launch {
            cache.observe(endpoint("subnets"), Payload::class.java) { release.await(); response("new") }.take(2).toList(seen)
        }
        runCurrent()
        assertEquals("old", seen.single().value.value)
        assertTrue(seen.single().stale)
        release.complete(Unit)
        collect.join()
        assertEquals("new", seen.last().value.value)
        assertFalse(seen.last().stale)
    }

    @Test
    fun `stale observation joins an existing background flight without waiting for retry interval`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("subnets"), Payload::class.java) { response("old", "no-store") }
        wall += 30_000
        nanos += 30_000_000_000L
        val release = CompletableDeferred<Unit>()
        var requests = 0
        cache.get(endpoint("subnets"), Payload::class.java, allowStale = true) {
            requests++
            release.await()
            response("new")
        }
        runCurrent()
        val seen = mutableListOf<CachedResponse<Payload>>()
        val observer = async {
            cache.observe(endpoint("subnets"), Payload::class.java) { error("Duplicated request") }.take(2).toList(seen)
        }
        runCurrent()
        assertEquals(listOf("old"), seen.map { it.value.value })
        release.complete(Unit)
        runCurrent()

        assertTrue(observer.isCompleted)
        assertEquals(listOf("old", "new"), observer.await().map { it.value.value })
        assertEquals(1, requests)
    }

    @Test
    fun `stale observation publishes a background flight that finished before the observer could join it`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        cache.get(endpoint("subnets"), Payload::class.java) { response("old", "no-store") }
        wall += 30_000
        nanos += 30_000_000_000L
        val release = CompletableDeferred<Unit>()
        cache.get(endpoint("subnets"), Payload::class.java, allowStale = true) {
            release.await()
            response("new")
        }
        runCurrent()
        // Rendezvous delivery parks the observer right after it read the stale row, before it looks for a flight.
        val values = cache.observe(endpoint("subnets"), Payload::class.java) { error("Duplicated request") }
            .buffer(Channel.RENDEZVOUS)
            .produceIn(backgroundScope)
        runCurrent()

        // The flight stores its response and leaves the registry while the observer still holds the stale row.
        release.complete(Unit)
        runCurrent()
        assertEquals("old", values.receive().value.value)
        runCurrent()

        assertEquals("new", values.tryReceive().getOrNull()?.value?.value)
    }

    @Test
    fun `query ordering is normalized but accounts and pages remain isolated`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val endpoint = endpoint("operations/search")
        var calls = 0
        cache.get("$endpoint?page=1&accountSubject=owner".toHttpUrl(), Payload::class.java, metaAccountId = 1L) { calls++; response("page one") }
        val same = cache.get("$endpoint?accountSubject=owner&page=1".toHttpUrl(), Payload::class.java, metaAccountId = 1L) { calls++; response("duplicate") }
        cache.get("$endpoint?page=2&accountSubject=owner".toHttpUrl(), Payload::class.java, metaAccountId = 1L) { calls++; response("page two") }
        cache.get("$endpoint?page=1&accountSubject=other".toHttpUrl(), Payload::class.java, metaAccountId = 1L) { calls++; response("another account") }
        assertEquals("page one", same.value.value)
        assertEquals(3, calls)
        assertEquals(3, dao.rows.size)
    }

    @Test
    fun `incompatible stored payload is removed before refresh`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val endpoint = endpoint("subnets")
        cache.get(endpoint, Payload::class.java) { response("old") }
        val row = dao.rows.values.single()
        dao.rows[row.key] = row.copy(payload = "not json")
        assertNull(cache.peek(endpoint, Payload::class.java))
        assertTrue(dao.rows.isEmpty())
        assertEquals("new", cache.get(endpoint, Payload::class.java) { response("new") }.value.value)
    }

    @Test
    fun `response storage stays bounded after many distinct URLs`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        repeat(520) { index ->
            wall++
            cache.get(endpoint("subnets/$index/validators"), Payload::class.java) { response(index.toString()) }
        }
        assertEquals(512, dao.rows.size)
        assertNull(cache.peek(endpoint("subnets/0/validators"), Payload::class.java))
        assertEquals("519", cache.peek(endpoint("subnets/519/validators"), Payload::class.java)!!.value.value)
    }

    @Test
    fun `identical private URLs deduplicate only within the same meta account`() = runTest {
        val cache = createResponseTestCache(dao, backgroundScope, wallClock, monotonicClock)
        val url = (endpoint("operations/search").toString() + "?accountSubject=shared&page=1").toHttpUrl()
        val release = CompletableDeferred<Unit>()
        var firstCalls = 0
        val first = async { cache.get(url, Payload::class.java, metaAccountId = 1L) { firstCalls++; release.await(); response("first") } }
        runCurrent()
        val duplicate = async { cache.get(url, Payload::class.java, metaAccountId = 1L) { firstCalls++; response("duplicate") } }
        val other = async { cache.get(url, Payload::class.java, metaAccountId = 2L) { response("other wallet") } }
        runCurrent()
        assertEquals("other wallet", other.await().value.value)
        assertFalse(first.isCompleted)
        release.complete(Unit)
        assertEquals("first", first.await().value.value)
        assertEquals("first", duplicate.await().value.value)
        assertEquals(1, firstCalls)
        cache.invalidate { it.metaAccountId == 1L }
        assertNull(cache.peek(url, Payload::class.java, metaAccountId = 1L))
        assertEquals("other wallet", cache.peek(url, Payload::class.java, metaAccountId = 2L)!!.value.value)
    }

    @Test
    fun `local snapshots and invalidation tickets are isolated by wallet`() = runTest {
        val cache = createResponseTestCache(scope = backgroundScope)
        val key = BittensorCacheKeys.positions("chain", "shared coldkey")
        cache.saveSnapshot(key, 1L, Payload("first"))
        cache.saveSnapshot(key, 2L, Payload("second"))
        assertEquals("first", cache.readSnapshot(key, Payload::class.java, 1L)!!.value)
        assertEquals("second", cache.readSnapshot(key, Payload::class.java, 2L)!!.value)
        assertNull(cache.readSnapshot(key, Payload::class.java, null))
        cache.withSnapshotTicket(key, 1L) { firstTicket ->
            cache.withSnapshotTicket(key, 2L) { secondTicket ->
                cache.invalidate(keepStale = true) { it.metaAccountId == 1L }
                assertFalse(cache.saveSnapshot(key, 1L, Payload("obsolete"), firstTicket))
                assertTrue(cache.saveSnapshot(key, 2L, Payload("second updated"), secondTicket))
            }
        }
    }

    @Test
    fun `late response cannot repopulate a deleted meta account`() = runTest {
        val owners = mutableSetOf(1L, 2L)
        val ownerDao = InMemoryResponseCacheDao(owners)
        val cache = createResponseTestCache(ownerDao, backgroundScope, wallClock, monotonicClock)
        val url = (endpoint("operations/search").toString() + "?accountSubject=shared&page=1").toHttpUrl()
        val release = CompletableDeferred<Unit>()
        val first = async { runCatching { cache.get(url, Payload::class.java, metaAccountId = 1L) { release.await(); response("late") } } }
        runCurrent()
        cache.get(url, Payload::class.java, metaAccountId = 2L) { response("other") }
        ownerDao.deleteOwner(1L)
        release.complete(Unit)
        assertTrue(first.await().isFailure)
        assertNull(cache.peek(url, Payload::class.java, metaAccountId = 1L))
        assertEquals("other", cache.peek(url, Payload::class.java, metaAccountId = 2L)!!.value.value)
    }

    @Test
    fun `runtime holdings roundtrip through a new cache owner and restore as stale`() = runTest {
        val snapshotDao = InMemoryResponseCacheDao()
        val cache = createResponseTestCache(snapshotDao)
        val hotkey = io.novafoundation.nova.common.address.AccountIdKey(ByteArray(32) { 1 })
        val root = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition(hotkey, 10.toBigInteger())
        val subnet = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition(
            io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid.Subnet(1),
            hotkey,
            20.toBigInteger()
        )
        val positions = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions(listOf(root), listOf(subnet))
        cache.saveSnapshot(BittensorCacheKeys.positions("chain", "owner"), 1L, positions)
        val restarted = createResponseTestCache(snapshotDao)
        val restored = restarted.readSnapshot(
            BittensorCacheKeys.positions("chain", "owner"),
            io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions::class.java,
            1L
        )!!
        assertEquals(positions, restored)
        assertNull(restarted.readSnapshot(BittensorCacheKeys.positions("chain", "another owner"), positions.javaClass, 1L))
    }

    @Test
    fun `transaction retains stale holdings but rejects an older runtime write`() = runTest {
        val cache = createResponseTestCache()
        val key = BittensorCacheKeys.positions("chain", "owner")
        cache.saveSnapshot(key, 1L, Payload("last known"))
        cache.withSnapshotTicket(key, 1L) { ticket ->
            cache.invalidate(keepStale = true) { it.metaAccountId == 1L }
            assertFalse(cache.saveSnapshot(key, 1L, Payload("obsolete flight"), ticket))
        }
        assertEquals("last known", cache.readSnapshot(key, Payload::class.java, 1L)!!.value)
        cache.withSnapshotTicket(key, 1L) { ticket ->
            assertTrue(cache.saveSnapshot(key, 1L, Payload("confirmed"), ticket))
        }
    }
}
