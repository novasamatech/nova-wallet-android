package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorPricesSourceTest {

    private val dao = InMemoryResponseCacheDao()
    private val cache = createResponseTestCache(dao)
    private val logger = mock(Logger::class.java)
    private val subnet = BittensorNetuid.Subnet(1)
    private val otherSubnet = BittensorNetuid.Subnet(2)
    private val coldKey = ByteArray(32)
    private val hotKey = AccountIdKey(ByteArray(32) { 1 })
    private val prices = mapOf(subnet to BigInteger.TEN, otherSubnet to BigInteger.ONE)
    private val positions = BittensorStakePositions(
        root = emptyList(),
        subnets = prices.keys.map { BittensorSubnetPosition(it, hotKey, BigInteger.ONE) }
    )
    private var fetch: suspend () -> Map<BittensorNetuid.Subnet, BigInteger> = { prices }
    private var requests = 0
    private val priceSource = object : BittensorPortfolioPriceSource {
        override suspend fun prices(chainId: String, heldSubnets: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigInteger> {
            requests++
            return fetch()
        }
    }
    private val computationalCache = UnsharedComputationalCache()
    private val source = BittensorPricesSource(priceSource, cache, computationalCache)
    private val freshPositions = flowOf(BittensorPositionState(positions, BittensorDataStatus.Fresh))
    private val stalePositions = flowOf(BittensorPositionState(positions, BittensorDataStatus.Stale(IllegalStateException("Offline"))))

    @Test
    fun `completed prices survive restart and render offline without a request`() = runTest {
        source.observePrices("chain", coldKey, freshPositions, 1L).first { it.loaded && !it.stale }
        val restartedCache = createResponseTestCache(dao)
        val restarted = BittensorPricesSource(priceSource, restartedCache, computationalCache)
        fetch = { error("Offline") }

        val restored = restarted.observePrices("chain", coldKey, stalePositions, 1L).first { it.loaded }

        assertEquals(prices, restored.prices)
        assertTrue(restored.stale)
        assertEquals(1, requests)
    }

    @Test
    fun `partial refresh retains missing saved prices without replacing the durable snapshot`() = runTest {
        source.observePrices("chain", coldKey, freshPositions, 1L).first { it.loaded }
        fetch = { mapOf(subnet to BigInteger.valueOf(20)) }
        val updates = source.observePrices("chain", coldKey, freshPositions, 1L).toList()

        assertEquals(mapOf(subnet to BigInteger.valueOf(20), otherSubnet to BigInteger.ONE), updates.last().prices)
        assertTrue(updates.last().stale)
        val restartedCache = createResponseTestCache(dao)
        val restarted = BittensorPricesSource(priceSource, restartedCache, computationalCache)
        val saved = restarted.observePrices("chain", coldKey, stalePositions, 1L).first { it.loaded }
        assertEquals(prices, saved.prices)
    }

    @Test
    fun `failed refresh keeps saved prices visible as stale`() = runTest {
        source.observePrices("chain", coldKey, freshPositions, 1L).first { it.loaded }
        // The price source returns the subset available from backend/runtime; neither supplied a price.
        fetch = { emptyMap() }

        val updates = source.observePrices("chain", coldKey, freshPositions, 1L).toList()

        assertEquals(prices, updates.last().prices)
        assertTrue(updates.last().stale)
        assertTrue(updates.last().loaded)
    }

    @Test
    fun `another wallet or chain cannot restore the saved prices`() = runTest {
        source.observePrices("chain", coldKey, freshPositions, 1L).first { it.loaded }

        val otherWallet = source.observePrices("chain", coldKey, stalePositions, 2L).first { it.heldSubnets.isNotEmpty() }
        val otherChain = source.observePrices("other", coldKey, stalePositions, 1L).first { it.heldSubnets.isNotEmpty() }
        val otherAccount = source.observePrices("chain", ByteArray(32) { 2 }, stalePositions, 1L).first { it.heldSubnets.isNotEmpty() }

        listOf(otherWallet, otherChain, otherAccount).forEach {
            assertTrue(it.prices.isEmpty())
            assertFalse(it.loaded)
        }
    }

    @Test
    fun `invalidation rejects an older response while retaining the last snapshot`() = runTest {
        source.observePrices("chain", coldKey, freshPositions, 1L).first { it.loaded }
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        fetch = {
            started.complete(Unit)
            release.await()
            prices.mapValues { BigInteger.valueOf(99) }
        }
        val updates = mutableListOf<BittensorPortfolioPrices>()
        val observer = launch { source.observePrices("chain", coldKey, freshPositions, 1L).toList(updates) }
        started.await()
        source.invalidate("chain", coldKey, 1L)
        release.complete(Unit)
        observer.join()

        assertTrue(updates.none { BigInteger.valueOf(99) in it.prices.values })
        val saved = source.observePrices("chain", coldKey, stalePositions, 1L).first { it.loaded }
        assertEquals(prices, saved.prices)
        assertTrue(saved.stale)
    }

    @Test
    fun `invalidation also rejects an older partial response`() = runTest {
        source.observePrices("chain", coldKey, freshPositions, 1L).first { it.loaded }
        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        fetch = {
            started.complete(Unit)
            release.await()
            mapOf(subnet to BigInteger.valueOf(99))
        }
        val updates = mutableListOf<BittensorPortfolioPrices>()
        val observer = launch { source.observePrices("chain", coldKey, freshPositions, 1L).toList(updates) }
        started.await()
        source.invalidate("chain", coldKey, 1L)
        release.complete(Unit)
        observer.join()

        assertTrue(updates.none { BigInteger.valueOf(99) in it.prices.values })
    }

    @Test
    fun `invalidating one wallet does not reject another wallet in flight`() = runTest {
        val release = CompletableDeferred<Unit>()
        fetch = {
            release.await()
            prices
        }
        val updates = mutableListOf<BittensorPortfolioPrices>()
        val observer = launch { source.observePrices("chain", coldKey, freshPositions, 2L).toList(updates) }
        runCurrent()
        source.invalidate("chain", coldKey, 1L)
        release.complete(Unit)
        observer.join()

        assertEquals(prices, updates.last().prices)
        assertFalse(updates.last().stale)
    }

    // Persistence tests collect finite flows. Repository integration tests exercise real subscription sharing.
    private class UnsharedComputationalCache : ComputationalCache {

        override suspend fun <T> useCache(key: String, scope: CoroutineScope, computation: suspend CoroutineScope.() -> T): T =
            computation(scope)

        override fun <T> useSharedFlow(key: String, scope: CoroutineScope, flowLazy: suspend CoroutineScope.() -> Flow<T>): Flow<T> = flow {
            emitAll(flowLazy(scope))
        }
    }
}
