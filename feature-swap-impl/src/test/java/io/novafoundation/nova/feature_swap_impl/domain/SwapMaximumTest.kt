package io.novafoundation.nova.feature_swap_impl.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class SwapMaximumTest {

    private fun planks(value: String) = BigDecimal(value).movePointRight(10).toBigIntegerExact()

    @Test
    fun `locked balance covering ED is not deducted twice`() {
        assertEquals(planks("0.47007"), swapMaximum(planks("0.47143"), planks("0.97143"), planks("0.01"), planks("0.00136")))
    }

    @Test
    fun `unlocked account reserves ED before fee is available`() {
        assertEquals(planks("0.46143"), swapMaximum(planks("0.47143"), planks("0.47143"), planks("0.01"), planks("0")))
        assertEquals(planks("0.46007"), swapMaximum(planks("0.47143"), planks("0.47143"), planks("0.01"), planks("0.00136")))
    }

    @Test
    fun `partial coverage reserves only missing ED and ignores locked funds not counting towards ED`() {
        assertEquals(planks("0.46507"), swapMaximum(planks("0.47143"), planks("0.47643"), planks("0.01"), planks("0.00136")))
    }

    @Test
    fun `maximum is clamped to zero`() {
        assertEquals(planks("0"), swapMaximum(planks("0.001"), planks("0.001"), planks("0.01"), planks("0.00136")))
    }
}
