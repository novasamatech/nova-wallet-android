package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StableSwapTradeMinimumTest {

    private val poolId = 100.toBigInteger()

    private val firstAsset = 1.toBigInteger()

    private val secondAsset = 2.toBigInteger()

    private val runtimeMinimum = 1234.toBigInteger()

    private val loadMinimum: suspend () -> BigInteger = { runtimeMinimum }

    @Test
    fun `ordinary sell and buy retain the required runtime trading minimum`() = runBlocking {
        val edge = edge(firstAsset, secondAsset, loadMinimum)

        assertEquals(runtimeMinimum, edge.minAllowedAmountIn())
        assertEquals(runtimeMinimum, edge.minAllowedAmountOut())
    }

    @Test
    fun `deposit sell requires the input trading minimum while buying shares has no output minimum`() = runBlocking {
        var reads = 0
        val countedLoader: suspend () -> BigInteger = {
            reads++
            runtimeMinimum
        }
        val edge = edge(firstAsset, poolId, countedLoader)

        assertEquals(runtimeMinimum, edge.minAllowedAmountIn())
        assertNull(edge.minAllowedAmountOut())
        assertEquals(1, reads)
    }

    @Test
    fun `withdraw sell and buy have no trading minimum and do not read the constant`() = runBlocking {
        val unexpectedLoader: suspend () -> BigInteger = { throw AssertionError("Withdraw must not read MinTradingLimit") }
        val edge = edge(poolId, firstAsset, unexpectedLoader)

        assertNull(edge.minAllowedAmountIn())
        assertNull(edge.minAllowedAmountOut())
    }

    @Test
    fun `buying shares needs no trading constant even when it is unavailable`() = runBlocking {
        val unavailableLoader: suspend () -> BigInteger = { throw NoSuchElementException("MinTradingLimit is missing") }
        val edge = edge(firstAsset, poolId, unavailableLoader)

        assertNull(edge.minAllowedAmountOut())
    }

    @Test
    fun `ordinary sell and buy propagate a missing required constant`() = runBlocking {
        val failure = NoSuchElementException("MinTradingLimit is missing")
        val unavailableLoader: suspend () -> BigInteger = { throw failure }
        val edge = edge(firstAsset, secondAsset, unavailableLoader)

        assertMissingMinimum(failure) { edge.minAllowedAmountIn() }
        assertMissingMinimum(failure) { edge.minAllowedAmountOut() }
    }

    @Test
    fun `deposit sell propagates a missing required constant`() = runBlocking {
        val failure = NoSuchElementException("MinTradingLimit is missing")
        val unavailableLoader: suspend () -> BigInteger = { throw failure }
        val edge = edge(firstAsset, poolId, unavailableLoader)

        assertMissingMinimum(failure) { edge.minAllowedAmountIn() }
    }

    @Test
    fun `cancellation during a required minimum read is preserved`() = runBlocking {
        val cancellation = CancellationException("Leaving swap")
        val cancelledLoader: suspend () -> BigInteger = { throw cancellation }
        val edge = edge(firstAsset, secondAsset, cancelledLoader)

        try {
            edge.minAllowedAmountIn()
            throw AssertionError("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    private suspend fun assertMissingMinimum(expected: NoSuchElementException, read: suspend () -> BigInteger?) {
        try {
            read()
            throw AssertionError("A required unknown minimum must not become an unrestricted trade")
        } catch (actual: NoSuchElementException) {
            assertSame(expected, actual)
        }
    }

    private fun edge(from: HydraDxAssetId, to: HydraDxAssetId, minimumLoader: suspend () -> BigInteger): StableSwapQuotingSource.Edge {
        return TestEdge(
            fromAsset = from to FullChainAssetId("hydration", from.toInt()),
            toAsset = to to FullChainAssetId("hydration", to.toInt()),
            poolId = poolId,
            minimumLoader = minimumLoader
        )
    }

    private class TestEdge(
        override val fromAsset: RemoteAndLocalId,
        override val toAsset: RemoteAndLocalId,
        override val poolId: HydraDxAssetId,
        private val minimumLoader: suspend () -> BigInteger
    ) : StableSwapQuotingSource.Edge {

        override val from: FullChainAssetId = fromAsset.second

        override val to: FullChainAssetId = toAsset.second

        override suspend fun minTradingLimit(): BigInteger = minimumLoader()

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger = error("Quote is unused in minimum checks")

        override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>): Int = error("Route weight is unused in minimum checks")
    }
}
