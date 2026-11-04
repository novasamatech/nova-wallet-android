package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSettlementScope
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import io.novafoundation.nova.core_db.dao.ResponseCacheDao
import io.novafoundation.nova.core_db.model.ResponseCacheKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.lang.reflect.Proxy
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorOperationHistorySettlementTest {

    private val api = BittensorEstimatesRepositoryTest.EstimatesApi()

    private var baseUrl = "https://infra.test/".toHttpUrl()

    private val urls = InfrastructureUrls { baseUrl }

    private val backendSource = createBittensorTestSources(urls, nanoTime = { nowNanos }, nowMillis = { nowMillis }, estimatesApi = api)

    private val cache = mock(ComputationalCache::class.java)

    // Remote deadline stays ahead of this clock; transport TTL has separate source tests.
    private var nowNanos = System.nanoTime() - 1_000_000_000_000L

    private val clock = { nowNanos }

    private var nowMillis = 1_790_000_000_000L

    private val wallClock = { nowMillis }

    private val storedDeadlines = mutableMapOf<String, Long>()

    private val storedScopes = mutableMapOf<String, String>()

    private val subnetA = BittensorNetuid.Subnet(1)

    private val subnetB = BittensorNetuid.Subnet(2)

    private val scopeA = BittensorSettlementScope.Network(subnetA)

    private val scopeB = BittensorSettlementScope.Network(subnetB)

    private val preferences = Proxy.newProxyInstance(
        Preferences::class.java.classLoader,
        arrayOf(Preferences::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "contains" -> arguments[0] in storedDeadlines || arguments[0] in storedScopes
            "getLong" -> storedDeadlines[arguments[0]] ?: arguments[1]
            "getString" -> storedScopes[arguments[0]]
            "putString" -> {
                storedScopes[arguments[0] as String] = arguments[1] as String
                null
            }
            "putLong" -> {
                storedDeadlines[arguments[0] as String] = arguments[1] as Long
                null
            }
            "removeField" -> {
                storedDeadlines.remove(arguments[0])
                storedScopes.remove(arguments[0])
                null
            }
            else -> error("Unexpected preference access: ${method.name}")
        }
    } as Preferences

    private val repository = newRepository()

    @Test
    fun `buy in subnet A never marks subnet B or root as settling including pending reload`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val history = backendSource.historySource(sharedCache, preferences, nowNanos = clock, nowMillis = wallClock)
        val updates = observeNetworkStates(history)
        runCurrent()
        assertTrue(updates.values.all { it.last().result!!.isSuccess })
        val beforeTrade = updates.mapValues { it.value.size }
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }

        history.ownTradeSubmitted(requireNotNull(history.request("account", 1L)), scopeA)
        runCurrent()

        assertNull(updates.getValue(subnetA).last().result)
        assertTrue(updates.getValue(subnetA).last().isSettling)
        for (unaffected in listOf(subnetB, BittensorNetuid.Root)) {
            assertTrue(updates.getValue(unaffected).drop(beforeTrade.getValue(unaffected)).none { it.isSettling })
        }
        release.complete(Unit)
        runCurrent()
        assertTrue(updates.getValue(subnetA).last().isSettling)
        assertFalse(updates.getValue(subnetB).last().isSettling)
        assertFalse(updates.getValue(BittensorNetuid.Root).last().isSettling)
        assertTrue(updates.values.all { it.last().result!!.isSuccess })
        assertEquals(2, api.historyPages.size) // All networks consume the same account-history request.
    }

    @Test
    fun `root stake settlement affects only the root estimate`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val history = backendSource.historySource(sharedCache, preferences, nowNanos = clock, nowMillis = wallClock)
        val rootScope = BittensorSettlementScope.Network(BittensorNetuid.Root)
        history.ownTradeSubmitted(requireNotNull(history.request("account", 1L)), rootScope)
        val updates = observeNetworkStates(history)
        runCurrent()

        assertTrue(updates.getValue(BittensorNetuid.Root).all { it.isSettling })
        assertTrue(updates.getValue(subnetA).none { it.isSettling })
        assertTrue(updates.getValue(subnetB).none { it.isSettling })
        assertTrue(updates.values.all { it.last().result!!.isSuccess })
        assertEquals(1, api.historyPages.size)
    }

    @Test
    fun `root claim settlement affects root and every subnet estimate`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val history = backendSource.historySource(sharedCache, preferences, nowNanos = clock, nowMillis = wallClock)
        history.ownTradeSubmitted(requireNotNull(history.request("account", 1L)), BittensorSettlementScope.Account)
        val updates = observeNetworkStates(history)
        runCurrent()

        assertTrue(updates.values.all { states -> states.isNotEmpty() && states.all { it.isSettling } })
        assertTrue(updates.values.all { it.last().result!!.isSuccess })
        assertEquals(1, api.historyPages.size)
    }

    @Test
    fun `later subnet B trade does not extend subnet A settlement deadline`() = runTest {
        val live = observeSettlement()
        runCurrent()
        live.submit(scopeA)
        runCurrent()
        advanceClocks(120_000L)
        live.submit(scopeB)
        runCurrent()
        assertSettling(live, subnetA, subnetB)

        advanceClocks(180_000L) // A at 300s; B remains protected until 420s.
        refresh(live)
        assertSettling(live, subnetB)
        val requestsBefore = api.historyPages.size
        refresh(live)
        assertEquals(requestsBefore + 1, api.historyPages.size) // B still prevents account-wide cache reuse.

        advanceClocks(120_000L)
        refresh(live)
        assertSettling(live)
        val settledRequests = api.historyPages.size
        refresh(live)
        assertEquals(settledRequests, api.historyPages.size)
    }

    @Test
    fun `another trade in the same subnet extends its deadline`() = runTest {
        val live = observeSettlement()
        runCurrent()
        live.submit(scopeA)
        runCurrent()
        advanceClocks(120_000L)
        live.submit(scopeA)
        runCurrent()

        advanceClocks(180_000L)
        refresh(live)
        assertSettling(live, subnetA)

        advanceClocks(120_000L)
        refresh(live)
        assertSettling(live)
    }

    @Test
    fun `later subnet trade does not extend the account wide root claim deadline`() = runTest {
        val live = observeSettlement()
        runCurrent()
        live.submit(BittensorSettlementScope.Account)
        runCurrent()
        advanceClocks(120_000L)
        live.submit(scopeB)
        runCurrent()
        assertSettling(live, BittensorNetuid.Root, subnetA, subnetB)

        advanceClocks(180_000L)
        refresh(live)
        assertSettling(live, subnetB)

        advanceClocks(120_000L)
        refresh(live)
        assertSettling(live)
    }

    @Test
    fun `refresh started before subnet A deadline cannot release A when it completes after deadline`() = runTest {
        val live = observeSettlement()
        runCurrent()
        live.submit(scopeA)
        runCurrent()
        advanceClocks(120_000L)
        live.submit(scopeB)
        runCurrent()
        advanceClocks(179_999L)
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = {
            started.complete(Unit)
            release.await()
        }
        refresh(live)
        assertTrue(started.isCompleted)

        advanceClocks(1L)
        release.complete(Unit)
        runCurrent()
        assertSettling(live, subnetA, subnetB)

        api.beforeHistoryResponse = {}
        refresh(live)
        assertSettling(live, subnetB)
    }

    @Test
    fun `restart restores per subnet scope and distinct remaining deadlines`() = runTest {
        repository.ownTradeSubmitted(request(), scopeA)
        advanceClocks(120_000L)
        repository.ownTradeSubmitted(request(), scopeB)
        advanceClocks(80_000L)
        nowNanos = 17_000_000_000L // Different process/boot monotonic origin.
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        val restarted = observeSettlement()
        runCurrent()
        assertSettling(restarted, subnetA, subnetB)

        release.complete(Unit)
        runCurrent()
        assertSettling(restarted, subnetA, subnetB)
        advanceClocks(100_000L)
        refresh(restarted)
        assertSettling(restarted, subnetB)
        advanceClocks(120_000L)
        refresh(restarted)
        assertSettling(restarted)
        assertTrue(storedScopes.isEmpty())
        assertTrue(storedDeadlines.isEmpty())
    }

    @Test
    fun `restart during B window retains elapsed A until a successful post deadline refresh`() = runTest {
        repository.ownTradeSubmitted(request(), scopeA)
        advanceClocks(120_000L)
        repository.ownTradeSubmitted(request(), scopeB)
        advanceClocks(230_000L)
        api.failure = IllegalStateException("Indexer unavailable")
        val restarted = observeSettlement()
        runCurrent()
        assertSettling(restarted, subnetA, subnetB)
        assertTrue(restarted.updates.values.all { it.last().result!!.isFailure })

        api.failure = null
        refresh(restarted)
        assertSettling(restarted, subnetB)
        advanceClocks(70_000L)
        refresh(restarted)
        assertSettling(restarted)
    }

    @Test
    fun `evicting inactive metadata never revives an older request for the same account`() = runTest {
        val old = request("old")
        repeat(40) { request("other-$it") }
        assertTrue(runCatching { repository.history(old) }.isFailure)

        val recreated = request("old")
        assertTrue(recreated.generation > old.generation)
        assertTrue(runCatching { repository.history(old) }.isFailure)
        repository.history(recreated)
    }

    @Test
    fun `observation retains its generation and releases metadata when cancelled`() = runTest {
        val active = request("active")
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeHistory(active).collect {}
        }
        runCurrent()
        repeat(40) { request("other-$it") }
        assertEquals(active, request("active"))

        observer.cancelAndJoin()
        repeat(40) { request("new-$it") }
        assertTrue(request("active").generation > active.generation)
    }

    @Test
    fun `in-flight history keeps its metadata until completion`() = runTest {
        val active = request("active")
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        val load = async { repository.history(active) }
        runCurrent()
        repeat(40) { request("other-$it") }
        assertEquals(active, request("active"))
        release.complete(Unit)
        assertTrue(load.await().allProviderPagesRead)
    }

    @Test
    fun `portfolio cancellation wins over an earlier operations invalidation failure`() = runTest {
        val dao = InMemoryResponseCacheDao()
        var attempts = 0
        val failingDao = object : ResponseCacheDao by dao {
            override suspend fun keys(): List<ResponseCacheKey> {
                attempts += 1
                if (attempts == 1) error("operations unavailable")
                throw CancellationException("leaving")
            }
        }
        val sources = createBittensorTestSources(urls, dao = failingDao, scope = backgroundScope)
        val history = sources.operations
        val request = requireNotNull(history.request("account", metaAccountId = 1L))

        val failure = runCatching { history.ownTradeSubmitted(request) }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertTrue(requireNotNull(history.request("account", metaAccountId = 1L)).generation > request.generation)
    }

    @Test
    fun `no-store history preserves server quality without gaining a reusable lifetime`() = runTest {
        api.historyCacheControl = { "no-store" }
        val request = request()

        assertFalse(repository.history(request).stale)
        assertFalse(repository.history(request).stale)
        assertEquals(2, api.historyPages.size)
    }

    @Test
    fun `history quality comes from backend metadata independently of HTTP lifetime`() = runTest {
        api.freshness = "STALE"
        assertTrue(repository.history(request()).stale)
    }

    @Test
    fun `owner scoped history preserves a pending legacy trade window on upgrade`() = runTest {
        val legacyIdentity = (baseUrl.toString() + "|account").encodeToByteArray().blake2b256().toHexString()
        storedDeadlines["bittensor_history_settlement_$legacyIdentity"] = nowMillis + 300_000L
        val initial = request()
        repository.history(initial)
        repository.history(initial)
        assertEquals(2, api.historyPages.size)
        assertEquals(2, storedDeadlines.size)
    }

    @Test
    fun `same chain account in different wallets has separate history and settlement`() = runTest {
        val first = requireNotNull(repository.request("shared", metaAccountId = 1L))
        val second = requireNotNull(repository.request("shared", metaAccountId = 2L))
        val firstHistory = repository.history(first)
        val secondHistory = repository.history(second)
        assertEquals(firstHistory, secondHistory)
        assertEquals(2, api.historyPages.size)
        repository.ownTradeSubmitted(first)
        assertEquals(secondHistory, repository.history(second))
        assertEquals(2, api.historyPages.size)
        assertEquals(second, repository.request("shared", metaAccountId = 2L))
        assertTrue(runCatching { repository.history(first) }.isFailure)
    }

    @Test
    fun `settlement remains visible through loading and failures until a post barrier fetch succeeds`() = runTest {
        val initial = request()
        repository.ownTradeSubmitted(initial)
        val updates = mutableListOf<BittensorOperationHistorySource.Snapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeHistory(request()).toList(updates)
        }
        runCurrent()
        assertNull(updates.first().result)
        assertTrue(updates.all { it.isSettling })
        assertTrue(updates.last().result!!.isSuccess)

        nowNanos += 300_000_000_000L
        api.failure = IllegalStateException("offline")
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(updates.last().result!!.getOrThrow().stale)
        assertTrue(updates.last().isSettling)

        api.failure = null
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(updates.last().isSettling)
        release.complete(Unit)
        runCurrent()
        assertTrue(updates.last().result!!.isSuccess)
        assertFalse(updates.last().isSettling)

        repository.ownTradeSubmitted(request())
        runCurrent()
        assertTrue(updates.last().isSettling)
    }

    @Test
    fun `fetch started during settlement cannot release earned after crossing the deadline`() = runTest {
        repository.ownTradeSubmitted(request())
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        val updates = mutableListOf<BittensorOperationHistorySource.Snapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeHistory(request()).toList(updates)
        }
        runCurrent()
        nowNanos += 300_000_000_000L
        release.complete(Unit)
        runCurrent()
        assertTrue(updates.last().result!!.isSuccess)
        assertTrue(updates.last().isSettling)

        advanceTimeBy(60_000)
        runCurrent()
        assertFalse(updates.last().isSettling)
    }

    @Test
    fun `restart inside the own trade window keeps the history settling until it passes`() = runTest {
        repository.ownTradeSubmitted(request())
        nowNanos += 100_000_000_000L
        nowMillis += 100_000L
        val restarted = newRepository()
        val updates = mutableListOf<BittensorOperationHistorySource.Snapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            restarted.observeHistory(requireNotNull(restarted.request("account", metaAccountId = 1L))).toList(updates)
        }
        runCurrent()
        assertTrue(updates.last().result!!.isSuccess)
        assertTrue(updates.all { it.isSettling })

        nowNanos += 200_000_000_000L
        nowMillis += 200_000L
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(updates.last().result!!.isSuccess)
        assertFalse(updates.last().isSettling)
    }

    @Test
    fun `restart restores only the remaining window and forgets an expired one`() = runTest {
        repository.ownTradeSubmitted(request())
        nowMillis += 299_000L
        val restarted = newRepository()
        val settling = requireNotNull(restarted.request("account", metaAccountId = 1L))
        restarted.history(settling)
        restarted.history(settling)
        assertEquals(2, api.historyPages.size)

        nowNanos += 1_000_000_000L
        val settled = restarted.history(settling)
        assertEquals(settled, restarted.history(settling))
        assertEquals(3, api.historyPages.size)

        nowMillis += 1_000L
        val expired = newRepository()
        val request = requireNotNull(expired.request("account", metaAccountId = 1L))
        assertEquals(expired.history(request), expired.history(request))
        assertTrue(storedDeadlines.isEmpty())
        assertEquals(3, api.historyPages.size)
    }

    @Test
    fun `settlement is account scoped and public stream clears immediately on own trade`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val repository = backendSource.historySource(sharedCache, preferences, nowNanos = clock, nowMillis = wallClock)
        val first = mutableListOf<BittensorHistoryState>()
        val second = mutableListOf<BittensorHistoryState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeState("first", metaAccountId = 1L, netuid = subnetA).toList(first)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeState("second", metaAccountId = 1L, netuid = subnetA).toList(second)
        }
        runCurrent()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        repository.ownTradeSubmitted(requireNotNull(repository.request("first", metaAccountId = 1L)))
        runCurrent()
        assertNull(first.last().result)
        assertTrue(first.last().isSettling)
        assertFalse(second.last().isSettling)
        release.complete(Unit)
        runCurrent()
        assertTrue(first.last().isSettling)
    }

    @Test
    fun `own trade drops cache and settlement remote results never seed cache`() = runTest {
        val initial = request()
        repository.history(initial)
        repository.ownTradeSubmitted(initial)
        val changed = request()

        repository.history(changed)
        repository.history(changed)
        assertEquals(3, api.historyPages.size)

        nowNanos += 300_000_000_000L
        val settled = repository.history(changed)
        assertEquals(settled, repository.history(changed))
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
        repository.history(changed)
        repository.history(changed)
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
        flight.await()

        api.beforeHistoryResponse = {}
        val settled = repository.history(changed)
        assertEquals(settled, repository.history(changed))
        assertEquals(2, api.historyPages.size)
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
        assertEquals(settled, repository.history(changed))
    }

    @Test
    fun `ordinary requests do not extend barrier but another own trade does`() = runTest {
        repository.ownTradeSubmitted(request())
        val changed = request()
        nowNanos += 299_000_000_000L
        repeat(30) { assertEquals(changed, request()) }
        nowNanos += 1_000_000_000L
        val settled = repository.history(changed)
        assertEquals(settled, repository.history(changed))

        repository.ownTradeSubmitted(changed)
        val next = request()
        repository.history(next)
        repository.history(next)
        assertEquals(3, api.historyPages.size)
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
        assertEquals(cachedOtherA, repository.history(otherA))
        assertTrue(runCatching { repository.history(b) }.exceptionOrNull() is IllegalStateException)

        baseUrl = a.baseUrl
        assertEquals(cachedB, repository.history(b))
        assertEquals(3, api.historyPages.size) // Returning to A reloads its pages after the origin changed.
        val changedA = request("a")
        repository.history(changedA)
        repository.history(changedA)
        assertEquals(5, api.historyPages.size)
    }

    @Test
    fun `live refresh keeps displayed value during pending request and recovers after failure`() = runTest {
        // This test exercises no-store admission, not settlement time travel. Use the
        // same monotonic clock as remote request deadlines so no-store cannot look fresh.
        val repository = backendSource.historySource(cache, preferences)
        val initial = requireNotNull(repository.request("account", metaAccountId = 1L))
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
        assertTrue(updates.last()!!.getOrThrow().stale)
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
    fun `screen cancellation does not cancel a shared validated response`() = runTest {
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
        assertEquals(1, api.historyPages.size)
    }

    @Test
    fun `a new subscription restores fresh pages immediately without refreshing or page spacing`() = runTest {
        api.nextHistoryPage = { if (it == 1) 2 else null }
        val request = request()
        val expected = repository.history(request)
        val restarted = backendSource.historySource(cache, preferences, spacingMillis = 12_000L, nowNanos = clock, nowMillis = wallClock)
        val restoredRequest = requireNotNull(restarted.request("account", metaAccountId = 1L))

        val restored = restarted.observeHistory(restoredRequest).first { it.result != null }

        assertEquals(expected, restored.result!!.getOrThrow())
        assertEquals(listOf(1, 2), api.historyPages)
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(expected, restarted.history(restoredRequest))
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun `foreground during a settlement request does not queue another request`() = runTest {
        val foreground = MutableStateFlow(true)
        val sources = createBittensorTestSources(urls, estimatesApi = api, scope = backgroundScope, foreground = foreground)
        val history = sources.historySource(cache, preferences, nowNanos = clock, nowMillis = wallClock)
        val initial = requireNotNull(history.request("account", metaAccountId = 1L))
        history.ownTradeSubmitted(initial)
        val request = requireNotNull(history.request("account", metaAccountId = 1L))
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            history.observeHistory(request).collect {}
        }
        runCurrent()
        assertEquals(1, api.historyPages.size)
        foreground.value = false
        runCurrent()
        foreground.value = true
        runCurrent()
        release.complete(Unit)
        runCurrent()
        assertEquals(1, api.historyPages.size)

        foreground.value = false
        runCurrent()
        foreground.value = true
        runCurrent()
        assertEquals(2, api.historyPages.size)
        observer.cancelAndJoin()
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(2, api.historyPages.size)
    }

    @Test
    fun `active history refresh follows its earliest page deadline`() = runTest {
        val now = { testScheduler.currentTime * 1_000_000L }
        val sources = createBittensorTestSources(urls, estimatesApi = api, scope = backgroundScope, nanoTime = now)
        val history = sources.historySource(cache, preferences, nowNanos = now)
        val request = requireNotNull(history.request("account", metaAccountId = 1L))
        api.nextHistoryPage = { if (it == 1) 2 else null }
        api.historyCacheControl = { if (it == 1) "max-age=60" else "max-age=30" }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { history.observeHistory(request).collect {} }
        runCurrent()
        assertEquals(listOf(1, 2), api.historyPages)

        advanceTimeBy(29_999L)
        runCurrent()
        assertEquals(listOf(1, 2), api.historyPages)
        advanceTimeBy(1L)
        runCurrent()
        assertEquals(listOf(1, 2, 2), api.historyPages)
    }

    @Test
    fun `origin and cached restoration failures are emitted without escaping the history producer`() = runTest {
        for (failingRead in 1..2) {
            val failure = IllegalStateException("config unavailable")
            var failReads = false
            var reads = 0
            val failingUrls = InfrastructureUrls {
                if (failReads && ++reads == failingRead) throw failure
                baseUrl
            }
            val sources = createBittensorTestSources(failingUrls, estimatesApi = api, scope = backgroundScope)
            val history = sources.historySource(cache, preferences)
            val request = requireNotNull(history.request("account", metaAccountId = 1L))
            failReads = true
            val requestsBefore = api.historyPages.size

            val updates = history.observeHistory(request).take(2).toList()

            assertEquals(failure, updates.first().result!!.exceptionOrNull())
            assertTrue(updates.last().result!!.isSuccess)
            assertEquals(requestsBefore + 1, api.historyPages.size)
        }
    }

    @Test
    fun `invalidation while the refresh loop wakes cannot escape the old generation producer`() = runTest {
        lateinit var history: BittensorOperationHistorySource
        lateinit var request: BittensorHistoryRequest
        var submitted = false
        val foreground = flow {
            emit(true) // Current foreground state is skipped by the cache.
            if (!submitted) {
                submitted = true
                history.ownTradeSubmitted(request)
                emit(true) // Wake the old loop before flatMapLatest processes the generation change.
            }
            awaitCancellation()
        }
        val sources = createBittensorTestSources(urls, estimatesApi = api, scope = backgroundScope, foreground = foreground)
        history = sources.historySource(cache, preferences, nowNanos = clock, nowMillis = wallClock)
        request = requireNotNull(history.request("account", metaAccountId = 1L))
        val updates = mutableListOf<BittensorOperationHistorySource.Snapshot>()
        val errors = mutableListOf<Throwable>()
        backgroundScope.launch {
            history.observeHistory(request).catch { errors += it }.toList(updates)
        }

        runCurrent()

        assertTrue(submitted)
        assertTrue(errors.isEmpty())
        assertEquals(2, api.historyPages.size)
        assertTrue(updates.last().request.generation > request.generation)
        assertTrue(updates.last().result!!.isSuccess)
        assertTrue(updates.last().isSettling)
    }

    @Test
    fun `fresh restored history refreshes at a subsecond remaining deadline`() = runTest {
        val now = { testScheduler.currentTime * 1_000_000L }
        val sources = createBittensorTestSources(urls, estimatesApi = api, scope = backgroundScope, nanoTime = now)
        val history = sources.historySource(cache, preferences, nowNanos = now)
        val request = requireNotNull(history.request("account", metaAccountId = 1L))
        history.history(request)
        advanceTimeBy(59_500L)
        backgroundScope.launch { history.observeHistory(request).collect {} }
        runCurrent()
        assertEquals(1, api.historyPages.size)

        advanceTimeBy(499L)
        runCurrent()
        assertEquals(1, api.historyPages.size)
        advanceTimeBy(1L)
        runCurrent()
        assertEquals(2, api.historyPages.size)
    }

    @Test
    fun `uncached settlement flight is rejected after backend changes away and back`() = runTest {
        repository.ownTradeSubmitted(request())
        val initial = request()
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { started.complete(Unit); release.await() }
        val old = async { runCatching { repository.history(initial) } }
        started.await()

        baseUrl = "https://other.test/".toHttpUrl()
        backendSource.client.baseUrl()
        baseUrl = initial.baseUrl
        backendSource.client.baseUrl()
        release.complete(Unit)

        assertTrue(old.await().isFailure)
    }

    private fun TestScope.observeNetworkStates(
        history: BittensorOperationHistorySource
    ): Map<BittensorNetuid, MutableList<BittensorHistoryState>> {
        val networks = listOf(BittensorNetuid.Root, subnetA, subnetB)
        val updates = networks.associateWith { mutableListOf<BittensorHistoryState>() }
        updates.forEach { (network, values) ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                history.observeState("account", metaAccountId = 1L, netuid = network).toList(values)
            }
        }
        return updates
    }

    private data class ObservedSettlement(
        val source: BittensorOperationHistorySource,
        val foreground: MutableStateFlow<Boolean>,
        val updates: Map<BittensorNetuid, MutableList<BittensorHistoryState>>
    )

    private fun TestScope.observeSettlement(): ObservedSettlement {
        val foreground = MutableStateFlow(true)
        val sources = createBittensorTestSources(
            urls = urls,
            estimatesApi = api,
            scope = backgroundScope,
            nanoTime = clock,
            nowMillis = wallClock,
            foreground = foreground
        )
        val sharedCache = SharedCache(backgroundScope)
        val history = sources.historySource(sharedCache, preferences, nowNanos = clock, nowMillis = wallClock)
        return ObservedSettlement(history, foreground, observeNetworkStates(history))
    }

    private suspend fun ObservedSettlement.submit(scope: BittensorSettlementScope) {
        source.ownTradeSubmitted(requireNotNull(source.request("account", metaAccountId = 1L)), scope)
    }

    private fun TestScope.refresh(live: ObservedSettlement) {
        live.foreground.value = false
        runCurrent()
        live.foreground.value = true
        runCurrent()
    }

    private fun assertSettling(live: ObservedSettlement, vararg affected: BittensorNetuid) {
        live.updates.forEach { (network, states) ->
            assertEquals("Settlement for netuid ${network.value}", network in affected, states.last().isSettling)
        }
    }

    private fun advanceClocks(millis: Long) {
        nowMillis += millis
        nowNanos += millis * 1_000_000L
    }

    private suspend fun request(address: String = "account") = requireNotNull(repository.request(address, metaAccountId = 1L))

    private fun newRepository() = backendSource.historySource(cache, preferences, nowNanos = clock, nowMillis = wallClock)

    @Test
    fun `history reuses shared page cache across more than eight accounts`() = runTest {
        val oldest = request("oldest")
        val nextOldest = request("next-oldest")
        val retained = repository.history(oldest)
        val second = repository.history(nextOldest)
        repeat(6) { repository.history(request("other-$it")) }

        assertEquals(retained, repository.history(oldest))
        repository.history(request("ninth"))

        assertEquals(retained, repository.history(oldest))
        assertEquals(nextOldest, request("next-oldest"))
        assertEquals(second, repository.history(nextOldest))
        assertEquals(9, api.historyPages.size)
    }

    @Test
    fun `concurrent history readers share transport and cannot race separate snapshots`() = runTest {
        val initial = request()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { started.complete(Unit); release.await() }
        val first = async { repository.history(initial) }
        started.await()
        val second = async { repository.history(initial) }
        runCurrent()
        assertEquals(1, api.historyPages.size)
        release.complete(Unit)
        assertEquals(first.await(), second.await())
        assertEquals(second.await(), repository.history(initial))
    }

    @Test
    fun `new subscriber never replays another origin and logged cache key contains no address`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val repository = backendSource.historySource(sharedCache, preferences, nowNanos = clock, nowMillis = wallClock)
        val first = mutableListOf<Result<BittensorOperationHistory>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe("private-account", metaAccountId = 1L).toList(first)
        }
        runCurrent()
        val fromA = first.last()!!.getOrThrow()
        baseUrl = "https://other.test/".toHttpUrl()
        val release = CompletableDeferred<Unit>()
        api.beforeHistoryResponse = { release.await() }
        val second = mutableListOf<Result<BittensorOperationHistory>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe("private-account", metaAccountId = 1L).toList(second)
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
        assertEquals(fromB, first.last()!!.getOrThrow())
        assertEquals(2, sharedCache.keys.size) // A did not migrate its producer under the old key.
    }

    @Test
    fun `other account reads preserve active generation and immediate invalidation while old load finishes`() = runTest {
        val sharedCache = SharedCache(backgroundScope)
        val repository = backendSource.historySource(sharedCache, preferences, nowNanos = clock, nowMillis = wallClock)
        val initial = requireNotNull(repository.request("active", metaAccountId = 1L))
        val updates = mutableListOf<Result<BittensorOperationHistory>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe("active", metaAccountId = 1L).toList(updates)
        }
        runCurrent()
        val first = updates.last()!!.getOrThrow()
        repeat(8) { repository.history(requireNotNull(repository.request("other-$it", metaAccountId = 1L))) }
        assertEquals(initial, repository.request("active", metaAccountId = 1L))
        assertEquals(first, repository.history(initial))
        assertEquals(9, api.historyPages.size)

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
