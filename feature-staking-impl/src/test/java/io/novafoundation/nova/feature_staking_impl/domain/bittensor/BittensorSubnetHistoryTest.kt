package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSubnetHistoryTest {

    @Test
    fun `uses contemporaneous native prices and sorts unsorted histories`() {
        val alpha = listOf(point(200, "12"), point(100, "10"))
        val native = listOf(point(200, "4"), point(100, "2"))

        val history = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY, nowSeconds = 200)

        assertEquals(listOf(100L, 200L), history.map { it.timestamp })
        assertEquals(listOf(BigDecimal("5.000000000000"), BigDecimal("3.000000000000")), history.map { it.tao })
    }

    @Test
    fun `does not invent a ratio for absent distant or zero native prices`() {
        val alpha = listOf(point(200_000, "10"))
        assertTrue(convertBittensorSubnetHistory(alpha, emptyList(), BittensorSubnetChartPeriod.DAY, nowSeconds = 200_000).isEmpty())
        assertTrue(convertBittensorSubnetHistory(alpha, listOf(point(1, "2")), BittensorSubnetChartPeriod.DAY, nowSeconds = 200_000).isEmpty())
        assertTrue(convertBittensorSubnetHistory(alpha, listOf(point(200_000, "0")), BittensorSubnetChartPeriod.DAY, nowSeconds = 200_000).isEmpty())
    }

    @Test
    fun `filters requested range relative to now and removes future samples`() {
        val alpha = listOf(point(1, "10"), point(200_000, "12"))
        val native = listOf(point(1, "2"), point(200_000, "4"))

        val history = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY, nowSeconds = 200_000)

        assertEquals(listOf(200_000L), history.map { it.timestamp })
        assertTrue(convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY, nowSeconds = 300_000).isEmpty())
        assertTrue(convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY, nowSeconds = 199_999).isEmpty())
    }

    @Test
    fun `prices before the current registration are dropped so a window starting earlier has no change`() {
        val day = 86_400L
        val now = 400 * day
        val registeredAt = now - 71 * day
        // A recycled netuid: the market quotes a full year, but the current subnet exists for 71 days only.
        val points = (0..365).map { BittensorSubnetPricePoint(now - (365 - it) * day, BigDecimal.ONE, BigDecimal.ONE) }

        assertTrue(hasBittensorSubnetHistoryCoverage(points, BittensorSubnetChartPeriod.YEAR, now))
        val clipped = clipBittensorSubnetHistory(points, registeredAt)
        assertTrue(clipped.all { it.timestamp >= registeredAt })
        assertFalse(hasBittensorSubnetHistoryCoverage(clipped, BittensorSubnetChartPeriod.YEAR, now))
        assertTrue(hasBittensorSubnetHistoryCoverage(clipped, BittensorSubnetChartPeriod.ALL, now))
        assertEquals(points, clipBittensorSubnetHistory(points, null))
    }

    @Test
    fun `registration time is the exact age before now rounded up to a whole second`() {
        assertEquals(1_000_000L - 32_400L, bittensorRegistrationEpochSeconds(9.hours, 1_000_000_000L))
        assertEquals(1_000_000L - 32_399L, bittensorRegistrationEpochSeconds(9.hours - 500.milliseconds, 1_000_000_000L))
    }

    @Test
    fun `a subnet registered nine hours ago keeps the prices quoted since its registration`() {
        val now = 1_000L * 86_400
        val points = hourlyPoints(hours = 48, now = now)

        val clipped = clipBittensorSubnetHistory(points, bittensorRegistrationEpochSeconds(9.hours, now * 1_000))

        assertEquals((0..9).map { now - (9 - it) * 3_600L }, clipped.map { it.timestamp })
    }

    @Test
    fun `a subnet registered less than a week ago has no weekly change`() {
        val now = 1_000L * 86_400
        val week = hourlyPoints(hours = 168, now = now)

        val clipped = clipBittensorSubnetHistory(week, bittensorRegistrationEpochSeconds(6.6.days, now * 1_000))

        assertTrue(hasBittensorSubnetHistoryCoverage(week, BittensorSubnetChartPeriod.WEEK, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(clipped, BittensorSubnetChartPeriod.WEEK, now))
    }

    private fun hourlyPoints(hours: Int, now: Long) = (0..hours).map {
        BittensorSubnetPricePoint(now - (hours - it) * 3_600L, BigDecimal.ONE, BigDecimal.ONE)
    }

    private fun point(timestamp: Long, price: String) = HistoricalCoinRate(timestamp, price.toBigDecimal())

    @Test
    fun `matches the nearest native sample including a newer sample`() {
        val alpha = listOf(point(490, "12"))
        val native = listOf(point(100, "2"), point(500, "4"))

        val history = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.DAY, nowSeconds = 500)

        assertEquals(BigDecimal("3.000000000000"), history.single().tao)
    }

    @Test
    fun `daily history never matches a native sample more than five minutes away`() {
        val alpha = listOf(point(1000, "12"))

        assertTrue(convertBittensorSubnetHistory(alpha, listOf(point(699, "4")), BittensorSubnetChartPeriod.DAY, nowSeconds = 1000).isEmpty())
        assertEquals(1, convertBittensorSubnetHistory(alpha, listOf(point(700, "4")), BittensorSubnetChartPeriod.DAY, nowSeconds = 1000).size)
    }

    @Test
    fun `coverage requires both current window boundaries within two sampling intervals`() {
        val now = 10_000_000L
        val start = now - 30 * 86400
        fun series(first: Long, last: Long) = listOf(
            BittensorSubnetPricePoint(first, BigDecimal.ONE, BigDecimal.ONE),
            BittensorSubnetPricePoint(last, BigDecimal.TEN, BigDecimal.TEN)
        )
        assertTrue(hasBittensorSubnetHistoryCoverage(series(start, now), BittensorSubnetChartPeriod.MONTH, now))
        assertTrue(hasBittensorSubnetHistoryCoverage(series(start - 7200, now), BittensorSubnetChartPeriod.MONTH, now))
        assertTrue(hasBittensorSubnetHistoryCoverage(series(start + 7200, now - 7200), BittensorSubnetChartPeriod.MONTH, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(start + 7201, now), BittensorSubnetChartPeriod.MONTH, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(start - 7201, now), BittensorSubnetChartPeriod.MONTH, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(start, now - 7201), BittensorSubnetChartPeriod.MONTH, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(start, now + 1), BittensorSubnetChartPeriod.MONTH, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(now - 86400, now), BittensorSubnetChartPeriod.MONTH, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(now + 1, now + 2), BittensorSubnetChartPeriod.MONTH, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(now + 1, now), BittensorSubnetChartPeriod.ALL, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(series(start - 86400, now - 86400), BittensorSubnetChartPeriod.MONTH, now))
    }

    @Test
    fun `all preserves history older than a year but still rejects stale and future endpoints`() {
        val now = 800L * 86400
        val alpha = listOf(point(1, "10"), point(now, "12"), point(now + 1, "13"))
        val native = listOf(point(1, "2"), point(now, "4"))
        val all = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.ALL, now)
        val year = convertBittensorSubnetHistory(alpha, native, BittensorSubnetChartPeriod.YEAR, now)
        assertEquals(listOf(1L, now), all.map { it.timestamp })
        assertEquals(listOf(now), year.map { it.timestamp })
        assertTrue(hasBittensorSubnetHistoryCoverage(all, BittensorSubnetChartPeriod.ALL, now))
        assertFalse(hasBittensorSubnetHistoryCoverage(all, BittensorSubnetChartPeriod.ALL, now + 2 * 86400 + 1))
        assertFalse(hasBittensorSubnetHistoryCoverage(all, BittensorSubnetChartPeriod.ALL, now - 1))
        assertEquals(
            listOf(
                BittensorSubnetChartPeriod.DAY,
                BittensorSubnetChartPeriod.WEEK,
                BittensorSubnetChartPeriod.MONTH,
                BittensorSubnetChartPeriod.YEAR,
                BittensorSubnetChartPeriod.ALL
            ),
            BittensorSubnetChartPeriod.entries
        )
    }
}
