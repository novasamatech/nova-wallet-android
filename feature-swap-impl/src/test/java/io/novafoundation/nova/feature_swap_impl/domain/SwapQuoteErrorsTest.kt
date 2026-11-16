package io.novafoundation.nova.feature_swap_impl.domain

import android.util.Log
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.graph.Graph
import io.novafoundation.nova.common.utils.graph.create
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.graph.WeightedEdge
import io.novafoundation.nova.common.utils.graph.EdgeVisitFilter
import io.novafoundation.nova.feature_swap_core.domain.paths.RealPathQuoterFactory
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.io.IOException
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockStatic

class SwapQuoteErrorsTest {
    private val input = asset(0)
    private val output = asset(2)
    private val cache = TestCache()
    private val factory = RealPathQuoterFactory(cache)

    @Test
    fun `network failure is preserved instead of reported as insufficient liquidity`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val failure = IOException("offline")
            val quoter = factory.create(flowOf(Graph.create(listOf(edge { throw failure }))), this, null, null)
            assertSame(failure, runCatching { quoter.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN) }.exceptionOrNull())
        }
    }

    @Test
    fun `a successful alternative route survives another pool failing`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val quoter = factory.create(flowOf(Graph.create(listOf(edge { throw IOException() }, edge { BigInteger.valueOf(25) }))), this, null, null)
            assertEquals(1, quoter.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN).candidates.size)
        }
    }

    @Test
    fun `no route is distinct from a route without liquidity`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val noRoute = factory.create(flowOf(Graph.create(emptyList<QuotableEdge>())), this, null, null)
            assertSame(
                SwapQuoteException.NoRoute,
                runCatching { noRoute.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN) }.exceptionOrNull()
            )
            val noLiquidity = factory.create(flowOf(Graph.create(listOf(edge { throw SwapQuoteException.NotEnoughLiquidity }))), this, null, null)
            assertSame(
                SwapQuoteException.NotEnoughLiquidity,
                runCatching { noLiquidity.findBestPath(input, output, BigInteger.TEN, SwapDirection.SPECIFIED_IN) }.exceptionOrNull()
            )
        }
    }

    @Test
    fun `account-specific visit filters never reuse another quoters paths`() = runBlocking<Unit> {
        mockStatic(Log::class.java).use {
            val graph = flowOf(Graph.create(listOf(edge { BigInteger.TEN })))
            factory.create(graph, this, null, null).findBestPath(input, output, BigInteger.ONE, SwapDirection.SPECIFIED_IN)
            val blocked = factory.create(graph, this, null, EdgeVisitFilter { _, _ -> false })
            assertSame(
                SwapQuoteException.NoRoute,
                runCatching { blocked.findBestPath(input, output, BigInteger.ONE, SwapDirection.SPECIFIED_IN) }.exceptionOrNull()
            )
        }
    }

    private fun edge(quote: suspend () -> BigInteger) = object : QuotableEdge {
        override val from = input.fullId
        override val to = output.fullId
        override fun weightForAppendingTo(path: Path<WeightedEdge<FullChainAssetId>>) = 100
        override suspend fun quote(amount: BigInteger, direction: SwapDirection) = quote()
    }

    private fun asset(id: Int) = mock(Chain.Asset::class.java).also {
        `when`(it.chainId).thenReturn("chain")
        `when`(it.id).thenReturn(id)
    }

    private class TestCache : ComputationalCache {
        private val values = mutableMapOf<String, Any?>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> useCache(key: String, scope: CoroutineScope, computation: suspend CoroutineScope.() -> T): T {
            if (!values.containsKey(key)) values[key] = computation(scope)
            return values[key] as T
        }
        override fun <T> useSharedFlow(key: String, scope: CoroutineScope, flowLazy: suspend CoroutineScope.() -> Flow<T>): Flow<T> = error("Not used")
    }
}
