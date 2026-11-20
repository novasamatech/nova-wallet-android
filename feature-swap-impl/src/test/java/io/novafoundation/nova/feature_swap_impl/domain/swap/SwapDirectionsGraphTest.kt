package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.common.utils.graph.findDijkstraPathsBetween
import io.novafoundation.nova.common.utils.graph.numberOfEdges
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class SwapDirectionsGraphTest {

    private val dot = FullChainAssetId("asset-hub", 0)

    private val usdt = FullChainAssetId("asset-hub", 1)

    private val hydraDot = FullChainAssetId("hydration", 0)

    @Test
    fun `first graph waits for asset hub routes instead of reporting a missing DOT USDT path`() = runTest {
        val assetHubLoaded = CompletableDeferred<Unit>()
        val crossChain = edge(dot, hydraDot)
        val assetHub = edge(dot, usdt)
        val directions = listOf(
            flowOf(listOf(crossChain)),
            flow {
                assetHubLoaded.await()
                emit(listOf(assetHub))
            }
        )

        val graph = async { combineSwapDirections(directions).first() }
        runCurrent()
        assertFalse(graph.isCompleted)

        assetHubLoaded.complete(Unit)
        val paths = graph.await().findDijkstraPathsBetween(dot, usdt, limit = 10)
        assertEquals(listOf(listOf(assetHub)), paths)
        assertEquals(2, graph.await().numberOfEdges())
    }

    @Test
    fun `loaded empty exchanges produce an empty graph rather than leaving the quote loading`() = runTest {
        val directions = listOf(flowOf(emptyList<SwapGraphEdge>()), flowOf(emptyList<SwapGraphEdge>()))

        val graph = combineSwapDirections(directions).first()

        assertEquals(0, graph.numberOfEdges())
        assertTrue(graph.findDijkstraPathsBetween(dot, usdt, limit = 10).isEmpty())
    }

    @Test
    fun `no exchanges also produces a completed empty graph`() = runTest {
        assertEquals(0, combineSwapDirections(emptyList()).first().numberOfEdges())
    }

    @Test
    fun `cancelling the graph collection cancels pending exchange loading`() = runTest {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val pending = flow<List<SwapGraphEdge>> {
            started.complete(Unit)
            try {
                CompletableDeferred<Unit>().await()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val graph = async { combineSwapDirections(listOf(pending)).first() }
        started.await()

        graph.cancelAndJoin()

        assertTrue(cancelled.isCompleted)
    }

    private fun edge(from: FullChainAssetId, to: FullChainAssetId): SwapGraphEdge {
        return mock(SwapGraphEdge::class.java).also {
            `when`(it.from).thenReturn(from)
            `when`(it.to).thenReturn(to)
        }
    }
}
