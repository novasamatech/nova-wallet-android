package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSubnetSparklineTest {

    @Test
    fun `empty and single observation do not draw a fabricated chart`() {
        assertTrue(normalizedBittensorSparkline(emptyList()).isEmpty())
        assertTrue(normalizedBittensorSparkline(listOf(BigDecimal.ONE)).isEmpty())
    }

    @Test
    fun `equal prices draw a centered horizontal line`() {
        assertEquals(listOf(0.5f, 0.5f), normalizedBittensorSparkline(listOf(BigDecimal.TEN, BigDecimal.TEN)))
    }

    @Test
    fun `normalization preserves shape without losing tiny token prices`() {
        val prices = listOf("0.000000000001", "0.000000000003", "0.000000000002").map(String::toBigDecimal)
        assertEquals(listOf(0f, 1f, 0.5f), normalizedBittensorSparkline(prices))
    }

    @Test
    fun `normalization does not overflow when raw amounts exceed float range`() {
        val largePrice = BigDecimal.TEN.pow(100)
        assertEquals(listOf(0f, 1f), normalizedBittensorSparkline(listOf(largePrice, largePrice * BigDecimal.TEN)))
    }
}
