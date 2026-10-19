package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSubnetHistoryTest {

    @Test
    fun `uses contemporaneous native prices and sorts unsorted histories`() {
        val alpha = listOf(point(200, "12"), point(100, "10"))
        val native = listOf(point(200, "4"), point(100, "2"))

        val history = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY)

        assertEquals(listOf(100L, 200L), history.map { it.timestamp })
        assertEquals(listOf(BigDecimal("5.000000000000"), BigDecimal("3.000000000000")), history.map { it.tao })
    }

    @Test
    fun `does not invent a ratio for absent distant or zero native prices`() {
        val alpha = listOf(point(200_000, "10"))
        assertTrue(convertBittensorSubnetHistory(alpha, emptyList(), BittensorSubnetChartPeriod.DAY).isEmpty())
        assertTrue(convertBittensorSubnetHistory(alpha, listOf(point(1, "2")), BittensorSubnetChartPeriod.DAY).isEmpty())
        assertTrue(convertBittensorSubnetHistory(alpha, listOf(point(200_000, "0")), BittensorSubnetChartPeriod.DAY).isEmpty())
    }

    @Test
    fun `filters the requested range relative to the latest alpha sample`() {
        val alpha = listOf(point(1, "10"), point(200_000, "12"))
        val native = listOf(point(1, "2"), point(200_000, "4"))

        val history = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY)

        assertEquals(listOf(200_000L), history.map { it.timestamp })
    }

    private fun point(timestamp: Long, price: String) = HistoricalCoinRate(timestamp, price.toBigDecimal())

    @Test
    fun `matches the nearest native sample including a newer sample`() {
        val alpha = listOf(point(490, "12"))
        val native = listOf(point(100, "2"), point(500, "4"))

        val history = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY)

        assertEquals(BigDecimal("3.000000000000"), history.single().tao)
    }

    @Test
    fun `daily history never matches a native sample more than five minutes away`() {
        val alpha = listOf(point(1000, "12"))

        assertTrue(convertBittensorSubnetHistory(alpha, listOf(point(699, "4")), BittensorSubnetChartPeriod.DAY).isEmpty())
        assertEquals(1, convertBittensorSubnetHistory(alpha, listOf(point(700, "4")), BittensorSubnetChartPeriod.DAY).size)
    }
}
