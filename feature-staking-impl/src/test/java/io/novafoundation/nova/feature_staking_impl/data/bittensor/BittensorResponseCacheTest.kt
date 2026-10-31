package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BittensorResponseCacheTest {

    private data class Payload(val value: String)

    private var base = "https://infra.test/".toHttpUrl()

    private val urls = InfrastructureUrls { base }

    private val dao = InMemoryBittensorCacheDao()

    private var wall = 1_000_000L

    private var nanos = 0L

    private val wallClock = { wall }

    private val monotonicClock = { nanos }

    private fun response(value: String, cacheControl: String = "max-age=60", age: String = "0") =
        Response.success(Payload(value), Headers.headersOf("Cache-Control", cacheControl, "Age", age))

    @Test
    fun `expiry subtracts Age caps at five minutes and starts before transport`() = runTest {
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        val result = cache.get("subnets", Payload::class.java) {
            wall += 20_000
            nanos += 20_000_000_000L
            response("saved", "public, max-age=600", "30")
        }
        assertEquals(1_000_000L, result.fetchedAt)
        assertEquals(1_270_000L, result.expiresAt)
        wall = 1_270_000L
        nanos = 270_000_000_000L
        assertTrue(cache.peek("subnets", Payload::class.java)!!.stale)
    }

    @Test
    fun `no store and no cache remain displayable without extending freshness`() = runTest {
        for (control in listOf("no-store", "no-cache, max-age=270")) {
            val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
            val result = cache.get(control, Payload::class.java) { response("stale-first", control) }
            assertTrue(result.stale)
            assertEquals(result.fetchedAt, result.expiresAt)
            val restarted = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
            assertEquals("stale-first", restarted.peek(control, Payload::class.java)!!.value.value)
            assertTrue(restarted.peek(control, Payload::class.java)!!.stale)
        }
    }

    @Test
    fun `identical concurrent requests share one flight while independent endpoints finish`() = runTest {
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val first = async { cache.get("validators", Payload::class.java) { calls++; release.await(); response("directory") } }
        runCurrent()
        val second = async { cache.get("validators", Payload::class.java) { calls++; response("duplicate") } }
        val recommendations = async { cache.get("recommendations", Payload::class.java) { response("recommendations") } }
        val root = async { cache.get("root/validators", Payload::class.java) { response("root") } }
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
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        cache.get("subnets", Payload::class.java) { response("old", "no-store") }
        wall += 30_000
        nanos += 30_000_000_000L
        val release = CompletableDeferred<Unit>()
        val seen = mutableListOf<BittensorCachedResponse<Payload>>()
        val collect = launch {
            cache.observe("subnets", Payload::class.java) { release.await(); response("new") }.take(2).toList(seen)
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
    fun `base URL change rejects old in flight response and never revives previous namespace`() = runTest {
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        val release = CompletableDeferred<Unit>()
        val old = async { runCatching { cache.get("subnets", Payload::class.java) { release.await(); response("old") } } }
        runCurrent()
        base = "https://other.test/".toHttpUrl()
        assertNull(cache.peek("subnets", Payload::class.java))
        cache.get("subnets", Payload::class.java) { response("new") }
        release.complete(Unit)
        assertTrue(old.await().isFailure)
        base = "https://infra.test/".toHttpUrl()
        assertNull(cache.peek("subnets", Payload::class.java))
    }

    @Test
    fun `transaction invalidation rejects in flight history without affecting another account`() = runTest {
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        cache.get("operations/search", Payload::class.java, account = "other") { response("other") }
        val release = CompletableDeferred<Unit>()
        val old = async {
            runCatching { cache.get("operations/search", Payload::class.java, account = "owner") { release.await(); response("before trade") } }
        }
        runCurrent()
        cache.invalidate(base, "owner", listOf("operations/search"))
        release.complete(Unit)
        assertTrue(old.await().isFailure)
        assertNull(cache.peek("operations/search", Payload::class.java, account = "owner"))
        assertEquals("other", cache.peek("operations/search", Payload::class.java, account = "other")!!.value.value)
        assertTrue(dao.rows.values.none { it.account.contains("other") || it.key.contains("other") })
    }

    @Test
    fun `alpha yield fan out is capped across concurrent consumers`() = runTest {
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        val release = CompletableDeferred<Unit>()
        var active = 0
        var maximum = 0
        val requests = (1..12).map { netuid ->
            async {
                cache.get("subnets/$netuid/yields/alpha", Payload::class.java) {
                    active++
                    maximum = maxOf(maximum, active)
                    release.await()
                    active--
                    response("yield")
                }
            }
        }
        runCurrent()
        assertEquals(4, maximum)
        release.complete(Unit)
        requests.forEach { it.await() }
        assertEquals(4, maximum)
    }

    @Test
    fun `runtime holdings roundtrip through a new cache owner and restore as stale`() = runTest {
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        val hotkey = io.novafoundation.nova.common.address.AccountIdKey(ByteArray(32) { 1 })
        val root = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition(hotkey, 10.toBigInteger())
        val subnet = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition(
            io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid.Subnet(1),
            hotkey,
            20.toBigInteger()
        )
        val positions = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions(listOf(root), listOf(subnet))
        cache.saveSnapshot("runtime/positions/chain", "owner", positions)
        val restarted = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        val restored = restarted.peek(
            "runtime/positions/chain",
            io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions::class.java,
            "owner"
        )!!
        assertEquals(positions, restored.value)
        assertTrue(restored.stale)
        assertNull(restarted.peek("runtime/positions/chain", positions.javaClass, "another owner"))
    }

    @Test
    fun `transaction retains stale holdings but rejects an older runtime write`() = runTest {
        val cache = createBittensorTestCache(urls, monotonicClock, wallClock, dao, backgroundScope)
        cache.saveSnapshot("runtime/positions/chain", "owner", Payload("last known"))
        val ticket = cache.snapshotTicket("owner")
        cache.invalidate(base, "owner", listOf("runtime/positions/chain"), keepStale = true)
        assertFalse(cache.saveSnapshot("runtime/positions/chain", "owner", Payload("obsolete flight"), ticket))
        assertEquals("last known", cache.peek("runtime/positions/chain", Payload::class.java, "owner")!!.value.value)
        assertTrue(cache.saveSnapshot("runtime/positions/chain", "owner", Payload("confirmed"), cache.snapshotTicket("owner")))
    }
}
