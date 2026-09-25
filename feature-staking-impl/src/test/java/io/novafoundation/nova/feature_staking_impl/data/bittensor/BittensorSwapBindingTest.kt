package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class BittensorSwapBindingTest {

    @Test
    fun `buy uses tao input and alpha output without subtracting runtime fees again`() {
        val result = bindBittensorSwapSimulation(simulation(100, 200), BittensorSubnetId(64), BittensorSwapDirection.TAO_TO_ALPHA, 102.bi())

        assertEquals(100.bi(), result.amountIn)
        assertEquals(200.bi(), result.amountOut)
        assertEquals(2.bi(), result.taoFee)
        assertEquals(3.bi(), result.alphaSlippage)
    }

    @Test
    fun `sell uses alpha input and tao output`() {
        val result = bindBittensorSwapSimulation(simulation(100, 200), BittensorSubnetId(64), BittensorSwapDirection.ALPHA_TO_TAO, 200.bi())

        assertEquals(200.bi(), result.amountIn)
        assertEquals(100.bi(), result.amountOut)
    }

    @Test
    fun `sell accounts for the alpha input fee without deducting output again`() {
        val decoded = simulation(100, 200, taoFee = 0, alphaFee = 3)
        val result = bindBittensorSwapSimulation(decoded, BittensorSubnetId(64), BittensorSwapDirection.ALPHA_TO_TAO, 203.bi())

        assertEquals(203.bi(), result.amountIn + result.alphaFee)
        assertEquals(100.bi(), result.amountOut)
    }

    @Test
    fun `zero output is a failed quote rather than a free trade`() {
        assertRejected(simulation(0, 0))
        assertRejected(simulation(100, 0))
    }

    @Test
    fun `real subnet 3 response decodes normalized fields and preserves runtime fees`() {
        val decoded = Struct.Instance(
            mapOf(
                "taoAmount" to 9_964_980.bi(),
                "alphaAmount" to 388_238_168.bi(),
                "taoFee" to 5_020.bi(),
                "alphaFee" to 0.bi(),
                "taoSlippage" to 0.bi(),
                "alphaSlippage" to 195_631.bi()
            )
        )
        val result = bindBittensorSwapSimulation(decoded, BittensorSubnetId(3), BittensorSwapDirection.TAO_TO_ALPHA, 9_970_000.bi())

        assertEquals(9_970_000.bi(), result.amountIn + result.taoFee)
        assertEquals(388_238_168.bi(), result.amountOut)
        assertEquals(195_631.bi(), result.alphaSlippage)
    }

    @Test
    fun `input fee is included when checking for impossible simulations`() {
        assertRejected(simulation(100, 200))
    }

    @Test
    fun `malformed and impossible values fail closed`() {
        assertRejected(Struct.Instance(emptyMap()))
        assertRejected(simulation(-1, 200))
        assertRejected(simulation(101, 200))
        assertTrue(runCatching { bindBittensorU64(BigInteger.ONE.shiftLeft(64)) }.isFailure)
    }

    private fun assertRejected(decoded: Any?) {
        assertTrue(runCatching {
            bindBittensorSwapSimulation(decoded, BittensorSubnetId(64), BittensorSwapDirection.TAO_TO_ALPHA, 100.bi())
        }.isFailure)
    }

    private fun simulation(tao: Long, alpha: Long, taoFee: Long = 2, alphaFee: Long = 0) = Struct.Instance(
        mapOf(
            "taoAmount" to tao.bi(),
            "alphaAmount" to alpha.bi(),
            "taoFee" to taoFee.bi(),
            "alphaFee" to alphaFee.bi(),
            "taoSlippage" to 0.bi(),
            "alphaSlippage" to 3.bi()
        )
    )

    private fun Long.bi() = BigInteger.valueOf(this)
    private fun Int.bi() = toLong().bi()
}
