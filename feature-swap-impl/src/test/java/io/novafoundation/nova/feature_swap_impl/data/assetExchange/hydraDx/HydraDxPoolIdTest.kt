package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.math.BigInteger

class HydraDxPoolIdTest {

    private val chainId = "hydration"

    private val usdc = FullChainAssetId(chainId, 1)
    private val hollar = FullChainAssetId(chainId, 2)
    private val dot = FullChainAssetId(chainId, 3)

    @Test
    fun `stableswap pool id is the same for every hop through the pool`() {
        assertEquals(
            HydraDxPoolId.stableswap(chainId, BigInteger.valueOf(110)),
            HydraDxPoolId.stableswap(chainId, BigInteger.valueOf(110))
        )
    }

    @Test
    fun `stableswap pool ids differ between pools`() {
        assertNotEquals(
            HydraDxPoolId.stableswap(chainId, BigInteger.valueOf(110)),
            HydraDxPoolId.stableswap(chainId, BigInteger.valueOf(111))
        )
    }

    @Test
    fun `omnipool is a single pool`() {
        assertEquals(HydraDxPoolId.omnipool(chainId), HydraDxPoolId.omnipool(chainId))
    }

    @Test
    fun `pair pool id does not depend on trade direction`() {
        assertEquals(
            HydraDxPoolId.pair(HydraDxPoolId.XYK, usdc, hollar),
            HydraDxPoolId.pair(HydraDxPoolId.XYK, hollar, usdc)
        )
    }

    @Test
    fun `pair pool ids differ between pairs and pool types`() {
        val xykUsdcHollar = HydraDxPoolId.pair(HydraDxPoolId.XYK, usdc, hollar)

        assertNotEquals(xykUsdcHollar, HydraDxPoolId.pair(HydraDxPoolId.XYK, usdc, dot))
        assertNotEquals(xykUsdcHollar, HydraDxPoolId.pair(HydraDxPoolId.AAVE, usdc, hollar))
    }
}
