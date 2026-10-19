package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorOperationHistorySettlementTest {

    private val api = BittensorEstimatesRepositoryTest.EstimatesApi()

    private val source = BittensorEstimatesRemoteSource(api, spacingMillis = 0)

    private var baseUrl = "https://infra.test/".toHttpUrl()

    private val urls = InfrastructureUrls { baseUrl }

    private val cache = mock(ComputationalCache::class.java)

    // Remote deadline stays ahead of this clock; transport TTL has separate source tests.
    private var nowNanos = System.nanoTime() - 1_000_000_000_000L

    private val clock = { nowNanos }

    private val repository = BittensorOperationHistoryRepository(source, urls, cache, clock)

    @Test
    fun `own trade drops cache and settlement remote results never seed cache`() = runTest {
        val initial = request()
        repository.history(initial)
        repository.ownTradeSubmitted(initial)
        val changed = request()

        val remote = repository.history(changed)
        assertNotSame(remote, repository.history(changed))
        assertEquals(3, api.historyPages.size)

        nowNanos += 300_000_000_000L
        val settled = repository.history(changed)
        assertSame(settled, repository.history(changed))
        assertEquals(4, api.historyPages.size)
    }

    @Test
    fun `old noncooperative flight is rejected after own trade and cannot seed cache`() = runTest {
        val oldRequest = request()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            started.complete(Unit)
            withContext(NonCancellable) { release.await() }
        }
        val oldFlight = async { runCatching { repository.history(oldRequest) } }
        started.await()
        repository.ownTradeSubmitted(oldRequest)
        release.complete(Unit)
        assertTrue(oldFlight.await().exceptionOrNull() is IllegalStateException)

        api.beforeHistoryResponse = {}
        val changed = request()
        assertNotSame(repository.history(changed), repository.history(changed))
        assertEquals(3, api.historyPages.size)
    }

    @Test
    fun `flight started during settlement cannot seed after barrier expires`() = runTest {
        repository.ownTradeSubmitted(request())
        val changed = request()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            started.complete(Unit)
            release.await()
        }
        val flight = async { repository.history(changed) }
        started.await()
        nowNanos += 300_000_000_000L
        release.complete(Unit)
        val lagged = flight.await()

        api.beforeHistoryResponse = {}
        val settled = repository.history(changed)
        assertNotSame(lagged, settled)
        assertSame(settled, repository.history(changed))
    }

    @Test
    fun `old flight finishing last cannot replace settled generation`() = runTest {
        val old = request()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            if (api.historyPages.size == 1) {
                started.complete(Unit)
                release.await()
            }
        }
        val flight = async { runCatching { repository.history(old) } }
        started.await()
        repository.ownTradeSubmitted(old)
        nowNanos += 300_000_000_000L
        val changed = request()
        val settled = repository.history(changed)
        release.complete(Unit)
        assertTrue(flight.await().isFailure)
        assertSame(settled, repository.history(changed))
    }

    @Test
    fun `ordinary requests do not extend barrier but another own trade does`() = runTest {
        repository.ownTradeSubmitted(request())
        val changed = request()
        nowNanos += 299_000_000_000L
        repeat(30) { assertEquals(changed, request()) }
        nowNanos += 1_000_000_000L
        val settled = repository.history(changed)
        assertSame(settled, repository.history(changed))

        repository.ownTradeSubmitted(changed)
        val next = request()
        assertNotSame(repository.history(next), repository.history(next))
    }

    @Test
    fun `captured submission invalidates only its original account and origin`() = runTest {
        val a = request("a")
        val b = request("b")
        val cachedB = repository.history(b)
        baseUrl = "https://other.test/".toHttpUrl()
        val otherA = request("a")
        val cachedOtherA = repository.history(otherA)
        repository.ownTradeSubmitted(a)
        assertSame(cachedOtherA, repository.history(otherA))
        assertTrue(runCatching { repository.history(b) }.exceptionOrNull() is IllegalStateException)

        baseUrl = a.baseUrl
        assertSame(cachedB, repository.history(b))
        val changedA = request("a")
        assertNotSame(repository.history(changedA), repository.history(changedA))
    }

    @Test
    fun `live refresh keeps displayed value during pending request and recovers after failure`() = runTest {
        // This test exercises no-store admission, not settlement time travel. Use the
        // same monotonic clock as remote request deadlines so no-store cannot look fresh.
        val repository = BittensorOperationHistoryRepository(source, urls, cache)
        val initial = requireNotNull(repository.request("account"))
        api.historyCacheControl = { "no-store" }
        val updates = mutableListOf<Result<BittensorOperationHistory>?>()
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeHistory(initial).map { it.result }.toList(updates)
        }
        runCurrent()
        assertEquals(2, updates.size)
        assertNull(updates.first())
        assertTrue(updates.last()!!.isSuccess)

        val release = CompletableDeferred<Unit>()
        val refreshStarted = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            refreshStarted.complete(Unit)
            release.await()
        }
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(refreshStarted.isCompleted)
        assertEquals(2, api.historyPages.size)
        assertEquals(2, updates.size) // no null/loading and no cancellation at the refresh boundary
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(2, updates.size)
        release.complete(Unit)
        runCurrent()
        assertEquals(3, updates.size)

        api.beforeHistoryResponse = {}
        api.failure = IllegalStateException("offline")
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(updates.last()!!.isFailure)
        api.failure = null
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(updates.last()!!.isSuccess)

        collection.cancel()
        val count = api.historyPages.size
        advanceTimeBy(600_000)
        runCurrent()
        assertEquals(count, api.historyPages.size)
    }

    @Test
    fun `own trade clears live value and immediately reloads without waiting for cadence`() = runTest {
        val updates = mutableListOf<Result<BittensorOperationHistory>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeHistory(request()).map { it.result }.toList(updates)
        }
        runCurrent()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        repository.ownTradeSubmitted(request())
        runCurrent()
        assertNull(updates.last())
        assertEquals(2, api.historyPages.size)
        release.complete(Unit)
        runCurrent()
        assertTrue(updates.last()!!.isSuccess)
    }

    @Test
    fun `cancelled noncooperative transport cannot seed cache`() = runTest {
        val initial = request()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            started.complete(Unit)
            withContext(NonCancellable) { release.await() }
        }
        val flight = async { repository.history(initial) }
        started.await()
        flight.cancel()
        release.complete(Unit)
        runCurrent()
        assertTrue(flight.isCancelled)
        api.beforeHistoryResponse = {}
        repository.history(initial)
        assertEquals(2, api.historyPages.size)
    }

    private suspend fun request(address: String = "account") = requireNotNull(repository.request(address))

    @Test
    fun `cache hits promote payloads so ninth entry evicts least recently used without losing generation`() = runTest {
        val oldest = request("oldest")
        val nextOldest = request("next-oldest")
        val retained = repository.history(oldest)
        val evicted = repository.history(nextOldest)
        repeat(6) { repository.history(request("other-$it")) }

        assertSame(retained, repository.history(oldest))
        repository.history(request("ninth"))

        assertSame(retained, repository.history(oldest))
        assertEquals(nextOldest, request("next-oldest"))
        assertNotSame(evicted, repository.history(nextOldest))
        assertEquals(10, api.historyPages.size)
    }

    @Test
    fun `older concurrent flight in same generation cannot replace newer cached result`() = runTest {
        val initial = request()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            if (api.historyPages.size == 1) {
                started.complete(Unit)
                release.await()
            }
        }
        val old = async { repository.history(initial) }
        started.await()
        val newer = repository.history(initial)
        release.complete(Unit)
        assertNotSame(newer, old.await())
        assertSame(newer, repository.history(initial))
    }

    @Test
    fun `new subscriber never replays another origin and logged cache key contains no address`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val repository = BittensorOperationHistoryRepository(source, urls, sharedCache, clock)
        val first = mutableListOf<Result<BittensorOperationHistory>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe("private-account").toList(first)
        }
        runCurrent()
        val fromA = first.last()!!.getOrThrow()
        baseUrl = "https://other.test/".toHttpUrl()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        val second = mutableListOf<Result<BittensorOperationHistory>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe("private-account").toList(second)
        }
        runCurrent()
        assertTrue(second.isNotEmpty())
        assertTrue(second.all { it == null })
        assertEquals(2, sharedCache.keys.size)
        assertTrue(sharedCache.keys.all { !it.contains("private-account") && !it.contains("https") })
        release.complete(Unit)
        runCurrent()
        val fromB = second.last()!!.getOrThrow()
        assertNotSame(fromA, fromB)
        advanceTimeBy(60_000)
        runCurrent()
        assertSame(fromB, first.last()!!.getOrThrow())
        assertEquals(2, sharedCache.keys.size) // A did not migrate its producer under the old key.
    }

    @Test
    fun `eight payload bound preserves active generation and immediate invalidation while old load finishes`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val repository = BittensorOperationHistoryRepository(source, urls, sharedCache, clock)
        val initial = requireNotNull(repository.request("active"))
        val updates = mutableListOf<Result<BittensorOperationHistory>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe("active").toList(updates)
        }
        runCurrent()
        val first = updates.last()!!.getOrThrow()
        repeat(8) { repository.history(requireNotNull(repository.request("other-$it"))) }
        assertEquals(initial, repository.request("active"))
        assertNotSame(first, repository.history(initial)) // oldest payload was evicted, not generation state

        api.historyCacheControl = { "no-store" }
        nowNanos = System.nanoTime() + 120_000_000_000L
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            started.complete(Unit)
            withContext(NonCancellable) { release.await() }
        }
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(started.isCompleted)
        repository.ownTradeSubmitted(initial)
        runCurrent()
        assertNull(updates.last()) // clears even before the noncooperative old flight is released
        val clearedAt = updates.size
        api.beforeHistoryResponse = {}
        release.complete(Unit)
        runCurrent()
        assertTrue(updates.last()!!.isSuccess)
        assertFalse(updates.drop(clearedAt).any { it?.getOrNull() === first })
    }

    private class SharedCache(private val scope: CoroutineScope) : ComputationalCache {

        private val flows = mutableMapOf<String, Flow<*>>()

        val keys: Set<String> get() = flows.keys

        override suspend fun <T> useCache(key: String, scope: CoroutineScope, computation: suspend CoroutineScope.() -> T): T =
            error("Not used")

        @Suppress("UNCHECKED_CAST")
        override fun <T> useSharedFlow(key: String, scope: CoroutineScope, flowLazy: suspend CoroutineScope.() -> Flow<T>): Flow<T> = flow {
            val shared = flows.getOrPut(key) {
                flowLazy(this@SharedCache.scope).shareIn(this@SharedCache.scope, SharingStarted.Eagerly, replay = 1)
            } as Flow<T>
            emitAll(shared)
        }
    }
}
