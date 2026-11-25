package io.novafoundation.nova.feature_swap_impl.domain.swap

import android.util.Log
import io.novafoundation.nova.feature_swap_impl.domain.swap.RealSwapService.Companion.directionsFor
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapAssetRole
import java.io.IOException
import kotlinx.coroutines.flow.toList
import org.mockito.Mockito.mockStatic
import io.novafoundation.nova.common.utils.graph.findAllPossibleDestinations
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

    @Test
    fun `a terminal hop permits Hydration to TAO but never TAO as a transit stop`() = runTest {
        val tao = FullChainAssetId("bittensor", 0)
        val xcm = edge(hydraDot, dot)
        val provider = edge(dot, tao).also { `when`(it.isTerminal).thenReturn(true) }
        val afterTao = edge(tao, usdt)
        val graph = combineSwapDirections(listOf(flowOf(listOf(xcm, provider, afterTao)))).first()

        val releaseGraph = combineSwapDirections(listOf(flowOf(listOf(xcm, provider)))).first()
        assertEquals(setOf(hydraDot, dot), releaseGraph.assetsForRole(SwapAssetRole.PAY))
        assertEquals(setOf(dot, tao), releaseGraph.assetsForRole(SwapAssetRole.RECEIVE))
        assertEquals(listOf(listOf(provider)), graph.findDijkstraPathsBetween(dot, tao, 1))
        assertEquals(listOf(listOf(xcm, provider)), graph.findDijkstraPathsBetween(hydraDot, tao, 1))
        assertTrue(graph.findDijkstraPathsBetween(hydraDot, usdt, 1).isEmpty())
        assertEquals(setOf(dot, tao), graph.findAllPossibleDestinations(hydraDot))

        val regular = edge(dot, tao)
        val parallel = combineSwapDirections(listOf(flowOf(listOf(xcm, provider, regular, afterTao)))).first()
        assertTrue(usdt in parallel.findAllPossibleDestinations(hydraDot))
        assertEquals(listOf(listOf(xcm, regular, afterTao)), parallel.findDijkstraPathsBetween(hydraDot, usdt, 1))
    }

    @Test
    fun `picker can use on-chain directions while a TAO quote awaits the provider`() = runTest {
        val tao = FullChainAssetId("bittensor", 0)
        val external = mock(AssetExchange.External::class.java)
        `when`(external.supportsDestination(tao)).thenReturn(true)
        val xcm = edge(hydraDot, dot)
        val provider = edge(dot, tao)
        val response = CompletableDeferred<List<SwapGraphEdge>>()
        val picker = requireNotNull(external.directionsFor(true, null) { response.await() })
        val quote = requireNotNull(external.directionsFor(true, tao) { response.await() })
        assertEquals(listOf(listOf(xcm)), combineSwapDirections(listOf(flowOf(listOf(xcm)), picker)).first().findDijkstraPathsBetween(hydraDot, dot, 1))
        val pending = async { quote.first() }
        runCurrent()
        assertFalse(pending.isCompleted)
        response.complete(listOf(provider))
        assertEquals(listOf(provider), pending.await())
        assertEquals(null, external.directionsFor(true, usdt) { error("unrelated provider requested") })
        assertEquals(null, external.directionsFor(false, tao) { error("external execution requested") })
    }

    @Test
    fun `catalogue failure preserves on-chain tokens and is retryable for TAO`() = runTest {
        mockStatic(Log::class.java).use {
            val external = mock(AssetExchange.External::class.java)
            `when`(external.supportsDestination(usdt)).thenReturn(true)
            val xcm = edge(hydraDot, dot)
            val failed = requireNotNull(external.directionsFor(true, null) { throw IOException("offline") })
            val paths = combineSwapDirections(listOf(flowOf(listOf(xcm)), failed)).toList()
            assertTrue(paths.all { it.findAllPossibleDestinations(hydraDot) == setOf(dot) })
            var requests = 0
            val quote = requireNotNull(external.directionsFor(true, usdt) {
                if (requests++ == 0) throw IOException("offline")
                listOf(xcm)
            })
            assertTrue(runCatching { quote.first() }.exceptionOrNull() is IOException)
            assertEquals(listOf(xcm), quote.first())
        }
    }

    private fun edge(from: FullChainAssetId, to: FullChainAssetId): SwapGraphEdge {
        return mock(SwapGraphEdge::class.java).also {
            `when`(it.from).thenReturn(from)
            `when`(it.to).thenReturn(to)
        }
    }
}
