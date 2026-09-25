package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class BittensorSlippageTest {

    @Test
    fun `buy allows a higher marginal price and sell a lower one`() {
        val slippage = BittensorSlippage(BigDecimal("0.01"))
        val price = BigInteger.valueOf(1000)

        assertEquals(BigInteger.valueOf(1010), slippage.limitPrice(price, BittensorSwapDirection.TAO_TO_ALPHA))
        assertEquals(BigInteger.valueOf(990), slippage.limitPrice(price, BittensorSwapDirection.ALPHA_TO_TAO))
    }

    @Test
    fun `rounding matches price limit direction`() {
        val slippage = BittensorSlippage(BigDecimal("0.01"))
        val price = BigInteger.valueOf(101)

        assertEquals(BigInteger.valueOf(103), slippage.limitPrice(price, BittensorSwapDirection.TAO_TO_ALPHA))
        assertEquals(BigInteger.valueOf(99), slippage.limitPrice(price, BittensorSwapDirection.ALPHA_TO_TAO))
    }

    @Test
    fun `zero slippage preserves exact price`() {
        for (direction in BittensorSwapDirection.entries) {
            assertEquals(BigInteger.TEN, BittensorSlippage(BigDecimal.ZERO).limitPrice(BigInteger.TEN, direction))
        }
    }

    @Test
    fun `invalid fractions and u64 limits are rejected`() {
        for (fraction in listOf("-0.01", "1", "2")) {
            assertTrue(runCatching { BittensorSlippage(BigDecimal(fraction)) }.isFailure)
        }
        val slippage = BittensorSlippage(BigDecimal("0.01"))
        assertTrue(runCatching { slippage.limitPrice(BigInteger.ZERO, BittensorSwapDirection.TAO_TO_ALPHA) }.isFailure)
        assertTrue(runCatching {
            slippage.limitPrice(BigInteger.ONE.shiftLeft(64) - BigInteger.ONE, BittensorSwapDirection.TAO_TO_ALPHA)
        }.isFailure)
        assertTrue(runCatching { slippage.limitPrice(BigInteger.ONE, BittensorSwapDirection.ALPHA_TO_TAO) }.isFailure)
    }
}
