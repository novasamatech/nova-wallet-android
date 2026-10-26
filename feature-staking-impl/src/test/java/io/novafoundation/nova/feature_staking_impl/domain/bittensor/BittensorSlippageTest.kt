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
        val slippage = BittensorSlippage(BigDecimal("0.005"))
        val price = BigInteger.valueOf(201)

        assertEquals(BigInteger.valueOf(202), slippage.limitPrice(price, BittensorSwapDirection.TAO_TO_ALPHA))
        assertEquals(BigInteger.valueOf(200), slippage.limitPrice(price, BittensorSwapDirection.ALPHA_TO_TAO))
    }

    @Test
    fun `zero and sub-rao slippage preserve a strict one rao boundary`() {
        for (fraction in listOf(BigDecimal.ZERO, BigDecimal("0.005"))) {
            val slippage = BittensorSlippage(fraction)
            assertEquals(BigInteger.valueOf(11), slippage.limitPrice(BigInteger.TEN, BittensorSwapDirection.TAO_TO_ALPHA))
            assertEquals(BigInteger.valueOf(9), slippage.limitPrice(BigInteger.TEN, BittensorSwapDirection.ALPHA_TO_TAO))
            assertEquals(BigInteger.TWO, slippage.limitPrice(BigInteger.ONE, BittensorSwapDirection.TAO_TO_ALPHA))
            assertTrue(runCatching { slippage.limitPrice(BigInteger.ONE, BittensorSwapDirection.ALPHA_TO_TAO) }.isFailure)
        }
    }

    @Test
    fun `strict boundary is checked for u64 overflow even with zero tolerance`() {
        val slippage = BittensorSlippage(BigDecimal.ZERO)
        val max = BigInteger.ONE.shiftLeft(64) - BigInteger.ONE
        assertTrue(runCatching { slippage.limitPrice(max, BittensorSwapDirection.TAO_TO_ALPHA) }.isFailure)
        assertEquals(max - BigInteger.ONE, slippage.limitPrice(max, BittensorSwapDirection.ALPHA_TO_TAO))
    }

    @Test
    fun `invalid fractions and u64 limits are rejected`() {
        for (fraction in listOf("-0.01", "1", "2")) {
            assertTrue(runCatching { BittensorSlippage(BigDecimal(fraction)) }.isFailure)
        }
        val slippage = BittensorSlippage(BigDecimal("0.01"))
        assertTrue(runCatching { slippage.limitPrice(BigInteger.ZERO, BittensorSwapDirection.TAO_TO_ALPHA) }.isFailure)
        assertTrue(
            runCatching {
                slippage.limitPrice(BigInteger.ONE.shiftLeft(64) - BigInteger.ONE, BittensorSwapDirection.TAO_TO_ALPHA)
            }.isFailure
        )
        assertTrue(runCatching { slippage.limitPrice(BigInteger.ONE, BittensorSwapDirection.ALPHA_TO_TAO) }.isFailure)
    }
}
