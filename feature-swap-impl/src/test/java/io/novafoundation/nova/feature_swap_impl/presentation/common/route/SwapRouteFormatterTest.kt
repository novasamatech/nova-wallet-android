package io.novafoundation.nova.feature_swap_impl.presentation.common.route

import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger

class SwapRouteFormatterTest {

    private val registry = mock(ChainRegistry::class.java)

    private val formatter = RealSwapRouteFormatter(registry)

    @Test
    fun `one network route remains visible for local swaps`() = runBlocking {
        assertEquals(listOf("A"), route("A", "A", "A"))
    }

    @Test
    fun `network transitions retain order including a return to the first chain`() = runBlocking {
        assertEquals(listOf("A", "B", "A"), route("A", "A", "B", "B", "A"))
    }

    private suspend fun route(vararg chainIds: String): List<String>? {
        val chains = chainIds.distinct().associateWith { id ->
            mock(Chain::class.java).also {
                `when`(it.id).thenReturn(id)
                `when`(it.name).thenReturn(id)
            }
        }
        `when`(registry.chainsById).thenReturn(MutableStateFlow(chains))

        val edges = chainIds.toList().zipWithNext { from, to ->
            val edge = mock(SwapGraphEdge::class.java)
            `when`(edge.from).thenReturn(FullChainAssetId(from, 0))
            `when`(edge.to).thenReturn(FullChainAssetId(to, 1))
            QuotedEdge(BigInteger.ONE, BigInteger.ONE, edge)
        }
        val path = QuotedPath(SwapDirection.SPECIFIED_IN, edges, PathRoughFeeEstimation.zero(), netAmountOut = BigInteger.ONE)
        val quote = mock(SwapQuote::class.java)
        `when`(quote.quotedPath).thenReturn(path)

        return formatter.formatSwapRoute(quote)?.chains?.map { it.id }
    }
}
