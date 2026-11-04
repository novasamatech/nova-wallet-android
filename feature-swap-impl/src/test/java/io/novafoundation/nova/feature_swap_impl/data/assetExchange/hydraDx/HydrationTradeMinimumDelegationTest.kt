package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.OmniPoolQuotingSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.StableSwapQuotingSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.XYKSwapQuotingSource
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.TradeAmountLimitedEdge
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.OmniPoolSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.StableSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.XYKSwapSourceFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class HydrationTradeMinimumDelegationTest {

    private val chain = mock(Chain::class.java)

    private val assetIdConverter = mock(HydraDxAssetIdConverter::class.java)

    private val stableFactory = StableSwapSourceFactory(assetIdConverter)

    private val omniFactory = OmniPoolSwapSourceFactory()

    private val xykFactory = XYKSwapSourceFactory()

    @Test
    fun `stableswap execution edge preserves quoting edge trade bounds`() = runBlocking {
        val edge = mock(StableSwapQuotingSource.Edge::class.java)
        val source = mock(StableSwapQuotingSource::class.java)
        stubBounds(edge, edge)
        `when`(chain.id).thenReturn("hydration")
        `when`(source.chain).thenReturn(chain)
        `when`(edge.poolId).thenReturn(100.toBigInteger())
        `when`(source.availableSwapDirections()).thenReturn(listOf(edge))

        assertBounds(stableFactory.create(source).availableSwapDirections().single())
    }

    @Test
    fun `omnipool execution edge preserves quoting edge trade bounds`() = runBlocking {
        val edge = mock(OmniPoolQuotingSource.Edge::class.java)
        val source = mock(OmniPoolQuotingSource::class.java)
        stubBounds(edge, edge)
        `when`(source.availableSwapDirections()).thenReturn(listOf(edge))

        assertBounds(omniFactory.create(source).availableSwapDirections().single())
    }

    @Test
    fun `xyk execution edge preserves minima alongside existing maxima`() = runBlocking {
        val edge = mock(XYKSwapQuotingSource.Edge::class.java)
        val source = mock(XYKSwapQuotingSource::class.java)
        stubBounds(edge, edge)
        `when`(source.availableSwapDirections()).thenReturn(listOf(edge))

        assertBounds(xykFactory.create(source).availableSwapDirections().single())
    }

    private suspend fun stubBounds(edge: QuotableEdge, limited: TradeAmountLimitedEdge) {
        `when`(edge.from).thenReturn(FullChainAssetId("hydration", 1))
        `when`(edge.to).thenReturn(FullChainAssetId("hydration", 2))
        `when`(limited.minAllowedAmountIn()).thenReturn(1000.toBigInteger())
        `when`(limited.minAllowedAmountOut()).thenReturn(2000.toBigInteger())
        `when`(limited.maxAllowedAmountIn()).thenReturn(3000.toBigInteger())
        `when`(limited.maxAllowedAmountOut()).thenReturn(4000.toBigInteger())
    }

    private suspend fun assertBounds(limited: TradeAmountLimitedEdge) {
        assertEquals(1000.toBigInteger(), limited.minAllowedAmountIn())
        assertEquals(2000.toBigInteger(), limited.minAllowedAmountOut())
        assertEquals(3000.toBigInteger(), limited.maxAllowedAmountIn())
        assertEquals(4000.toBigInteger(), limited.maxAllowedAmountOut())
    }
}
