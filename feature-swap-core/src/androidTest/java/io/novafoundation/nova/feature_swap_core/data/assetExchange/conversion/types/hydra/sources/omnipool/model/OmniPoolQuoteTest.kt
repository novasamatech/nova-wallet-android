package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Tests for [OmniPool.quote] with exact expected values sourced from hydra-dx-math Rust crate tests.
 * See: https://github.com/galacticcouncil/hydration-node/blob/42f3ae1d/math/src/omnipool/tests.rs
 */
class OmniPoolQuoteTest {

    companion object {
        private val UNIT = 1_000_000_000_000L.toBigInteger()

        private val ASSET_IN_ID = BigInteger.ZERO
        private val ASSET_OUT_ID = BigInteger.ONE
    }

    private fun buildPool(
        assetInReserve: BigInteger,
        assetInHubReserve: BigInteger,
        assetInShares: BigInteger,
        assetOutReserve: BigInteger,
        assetOutHubReserve: BigInteger,
        assetOutShares: BigInteger,
        assetInProtocolFee: Double = 0.0,
        assetOutAssetFee: Double = 0.0,
        maxSlipFee: Double = 0.0
    ): OmniPool {
        val tokenIn = OmniPoolToken(
            hubReserve = assetInHubReserve,
            shares = assetInShares,
            protocolShares = BigInteger.ZERO,
            tradeability = Tradeability(0b11.toBigInteger()),
            balance = assetInReserve,
            fees = OmniPoolFees(
                protocolFee = assetInProtocolFee.fractions,
                assetFee = Fraction.ZERO
            )
        )
        val tokenOut = OmniPoolToken(
            hubReserve = assetOutHubReserve,
            shares = assetOutShares,
            protocolShares = BigInteger.ZERO,
            tradeability = Tradeability(0b11.toBigInteger()),
            balance = assetOutReserve,
            fees = OmniPoolFees(
                protocolFee = Fraction.ZERO,
                assetFee = assetOutAssetFee.fractions
            )
        )

        return OmniPool(
            tokens = mapOf(ASSET_IN_ID to tokenIn, ASSET_OUT_ID to tokenOut),
            maxSlipFee = maxSlipFee.fractions
        )
    }

    // Rust: calculate_sell_should_work_when_correct_input_provided
    // asset_in: reserve=10*UNIT, hub=20*UNIT, shares=10*UNIT
    // asset_out: reserve=5*UNIT, hub=5*UNIT, shares=20*UNIT
    // amount=4*UNIT, fees=0
    @Test
    fun sellWithZeroFees() {
        val pool = buildPool(
            assetInReserve = 10.toBigInteger() * UNIT,
            assetInHubReserve = 20.toBigInteger() * UNIT,
            assetInShares = 10.toBigInteger() * UNIT,
            assetOutReserve = 5.toBigInteger() * UNIT,
            assetOutHubReserve = 5.toBigInteger() * UNIT,
            assetOutShares = 20.toBigInteger() * UNIT
        )

        val result = pool.quote(ASSET_IN_ID, ASSET_OUT_ID, 4.toBigInteger() * UNIT, SwapDirection.SPECIFIED_IN)

        // asset_out.delta_reserve = Decrease(2_666_666_666_666)
        assertEquals(2_666_666_666_666L.toBigInteger(), result)
    }

    // Rust: calculate_sell_with_fees_should_work_when_correct_input_provided
    // Same pool, asset_fee=1%, protocol_fee=1%
    @Test
    fun sellWithAssetAndProtocolFees() {
        val pool = buildPool(
            assetInReserve = 10.toBigInteger() * UNIT,
            assetInHubReserve = 20.toBigInteger() * UNIT,
            assetInShares = 10.toBigInteger() * UNIT,
            assetOutReserve = 5.toBigInteger() * UNIT,
            assetOutHubReserve = 5.toBigInteger() * UNIT,
            assetOutShares = 20.toBigInteger() * UNIT,
            assetOutAssetFee = 0.01,
            assetInProtocolFee = 0.01
        )

        val result = pool.quote(ASSET_IN_ID, ASSET_OUT_ID, 4.toBigInteger() * UNIT, SwapDirection.SPECIFIED_IN)

        // asset_out.delta_reserve = Decrease(2_627_613_941_018)
        assertEquals(2_627_613_941_018L.toBigInteger(), result)
    }

    // Rust: calculate_buy_should_work_when_correct_input_provided
    // Same pool, amount_to_buy=1*UNIT, fees=0
    @Test
    fun buyWithZeroFees() {
        val pool = buildPool(
            assetInReserve = 10.toBigInteger() * UNIT,
            assetInHubReserve = 20.toBigInteger() * UNIT,
            assetInShares = 10.toBigInteger() * UNIT,
            assetOutReserve = 5.toBigInteger() * UNIT,
            assetOutHubReserve = 5.toBigInteger() * UNIT,
            assetOutShares = 20.toBigInteger() * UNIT
        )

        val result = pool.quote(ASSET_IN_ID, ASSET_OUT_ID, 1.toBigInteger() * UNIT, SwapDirection.SPECIFIED_OUT)

        // asset_in.delta_reserve = Increase(666_666_666_668)
        assertEquals(666_666_666_668L.toBigInteger(), result)
    }

    // Rust: calculate_buy_with_fees_should_work_when_correct_input_provided
    // Same pool, asset_fee=1%, protocol_fee=1%
    @Test
    fun buyWithAssetAndProtocolFees() {
        val pool = buildPool(
            assetInReserve = 10.toBigInteger() * UNIT,
            assetInHubReserve = 20.toBigInteger() * UNIT,
            assetInShares = 10.toBigInteger() * UNIT,
            assetOutReserve = 5.toBigInteger() * UNIT,
            assetOutHubReserve = 5.toBigInteger() * UNIT,
            assetOutShares = 20.toBigInteger() * UNIT,
            assetOutAssetFee = 0.01,
            assetInProtocolFee = 0.01
        )

        val result = pool.quote(ASSET_IN_ID, ASSET_OUT_ID, 1.toBigInteger() * UNIT, SwapDirection.SPECIFIED_OUT)

        // asset_in.delta_reserve = Increase(682_966_807_814)
        assertEquals(682_966_807_814L.toBigInteger(), result)
    }

    // Rust: sell_with_slip_reduces_output
    // asset_in: reserve=10_000_000*UNIT, hub=10_000_000*UNIT, shares=10_000_000*UNIT
    // asset_out: reserve=500_000*UNIT, hub=5_000_000*UNIT, shares=500_000*UNIT
    @Test
    fun sellWithSlipFeeReducesOutput() {
        val noSlipPool = buildPool(
            assetInReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInHubReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInShares = 10_000_000L.toBigInteger() * UNIT,
            assetOutReserve = 500_000L.toBigInteger() * UNIT,
            assetOutHubReserve = 5_000_000L.toBigInteger() * UNIT,
            assetOutShares = 500_000L.toBigInteger() * UNIT,
            assetOutAssetFee = 0.0025,
            assetInProtocolFee = 0.0005
        )
        val slipPool = buildPool(
            assetInReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInHubReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInShares = 10_000_000L.toBigInteger() * UNIT,
            assetOutReserve = 500_000L.toBigInteger() * UNIT,
            assetOutHubReserve = 5_000_000L.toBigInteger() * UNIT,
            assetOutShares = 500_000L.toBigInteger() * UNIT,
            assetOutAssetFee = 0.0025,
            assetInProtocolFee = 0.0005,
            maxSlipFee = 0.05
        )

        val noSlipResult = noSlipPool.quote(ASSET_IN_ID, ASSET_OUT_ID, 100_000L.toBigInteger() * UNIT, SwapDirection.SPECIFIED_IN)!!
        val slipResult = slipPool.quote(ASSET_IN_ID, ASSET_OUT_ID, 100_000L.toBigInteger() * UNIT, SwapDirection.SPECIFIED_IN)!!

        assertTrue("Slip fee should reduce output: $slipResult < $noSlipResult", slipResult < noSlipResult)
    }

    // Rust: buy_with_slip_increases_cost
    // Same pool as slip sell test, amount_to_buy=1000*UNIT
    @Test
    fun buyWithSlipFeeIncreasesCost() {
        val noSlipPool = buildPool(
            assetInReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInHubReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInShares = 10_000_000L.toBigInteger() * UNIT,
            assetOutReserve = 500_000L.toBigInteger() * UNIT,
            assetOutHubReserve = 5_000_000L.toBigInteger() * UNIT,
            assetOutShares = 500_000L.toBigInteger() * UNIT,
            assetOutAssetFee = 0.0025,
            assetInProtocolFee = 0.0005
        )
        val slipPool = buildPool(
            assetInReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInHubReserve = 10_000_000L.toBigInteger() * UNIT,
            assetInShares = 10_000_000L.toBigInteger() * UNIT,
            assetOutReserve = 500_000L.toBigInteger() * UNIT,
            assetOutHubReserve = 5_000_000L.toBigInteger() * UNIT,
            assetOutShares = 500_000L.toBigInteger() * UNIT,
            assetOutAssetFee = 0.0025,
            assetInProtocolFee = 0.0005,
            maxSlipFee = 0.05
        )

        val noSlipResult = noSlipPool.quote(ASSET_IN_ID, ASSET_OUT_ID, 1_000L.toBigInteger() * UNIT, SwapDirection.SPECIFIED_OUT)!!
        val slipResult = slipPool.quote(ASSET_IN_ID, ASSET_OUT_ID, 1_000L.toBigInteger() * UNIT, SwapDirection.SPECIFIED_OUT)!!

        assertTrue("Slip fee should increase cost: $slipResult > $noSlipResult", slipResult > noSlipResult)
    }
}
