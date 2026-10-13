package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BittensorPortfolioValueSeriesTest {

    private val hour = 3600L
    private val windowStart = 1_000_000L
    private val windowEnd = windowStart + 7 * 24 * hour

    private fun point(offsetHours: Long, tao: String, completed: Boolean = true) =
        BittensorPortfolioStakePoint(windowStart + offsetHours * hour, BigDecimal(tao), BigDecimal.ZERO, completed)

    private fun history(vararg points: BittensorPortfolioStakePoint) =
        BittensorPortfolioStakeHistory(BittensorPortfolioHistoryPeriod.SEVEN_DAYS, windowStart, windowEnd, points.toList())

    private fun rate(offsetHours: Double, rate: String) = HistoricalCoinRate(windowStart + (offsetHours * hour).toLong(), BigDecimal(rate))

    private fun live(offsetHours: Long, tao: String, rate: String) = BittensorPortfolioLivePoint(windowStart + offsetHours * hour, BigDecimal(tao), BigDecimal(rate))

    @Test
    fun `completed points are priced with the nearest grid rate`() {
        val series = BittensorPortfolioValueSeriesCalculator.calculate(
            history(point(0, "10"), point(1, "12")),
            listOf(rate(0.5, "2"), rate(1.25, "3")),
            livePoint = null
        )
        assertEquals(listOf(BigDecimal("20"), BigDecimal("36")), series.points.map { it.fiatValue })
        assertEquals(listOf(windowStart, windowStart + hour), series.points.map { it.timestamp })
    }

    @Test
    fun `point without a grid rate within tolerance is skipped`() {
        val series = BittensorPortfolioValueSeriesCalculator.calculate(
            history(point(0, "10"), point(10, "12")),
            listOf(rate(0.0, "2"), rate(20.0, "3")),
            livePoint = null
        )
        assertEquals(listOf(windowStart), series.points.map { it.timestamp })
    }

    @Test
    fun `open bucket is dropped when a live point exists and kept otherwise`() {
        val grid = listOf(rate(0.0, "2"), rate(1.0, "2"))
        val withLive = BittensorPortfolioValueSeriesCalculator.calculate(history(point(0, "10"), point(1, "11", completed = false)), grid, live(2, "15", "4"))
        assertEquals(listOf(BigDecimal("20"), BigDecimal("60")), withLive.points.map { it.fiatValue })

        val withoutLive = BittensorPortfolioValueSeriesCalculator.calculate(history(point(0, "10"), point(1, "11", completed = false)), grid, livePoint = null)
        assertEquals(listOf(BigDecimal("20"), BigDecimal("22")), withoutLive.points.map { it.fiatValue })
    }

    @Test
    fun `live point is appended only when newer than the last point`() {
        val grid = listOf(rate(0.0, "2"), rate(5.0, "2"))
        val older = BittensorPortfolioValueSeriesCalculator.calculate(history(point(0, "10"), point(5, "11")), grid, live(5, "15", "4"))
        assertEquals(2, older.points.size)
        val newer = BittensorPortfolioValueSeriesCalculator.calculate(history(point(0, "10"), point(5, "11")), grid, live(6, "15", "4"))
        assertEquals(3, newer.points.size)
        assertEquals(BigDecimal("15"), newer.points.last().taoValue)
    }

    @Test
    fun `change is covered only when the first point is within two steps of the window start`() {
        val grid = listOf(rate(0.0, "2"), rate(2.0, "2"), rate(3.0, "2"), rate(100.0, "2"))
        assertTrue(BittensorPortfolioValueSeriesCalculator.calculate(history(point(2, "10"), point(100, "11")), grid, null).changeCovered)
        assertFalse(BittensorPortfolioValueSeriesCalculator.calculate(history(point(3, "10"), point(100, "11")), grid, null).changeCovered)
    }

    @Test
    fun `change is not covered for a single point or a zero start`() {
        val grid = listOf(rate(0.0, "2"), rate(100.0, "2"))
        assertFalse(BittensorPortfolioValueSeriesCalculator.calculate(history(point(0, "10")), grid, null).changeCovered)
        assertFalse(BittensorPortfolioValueSeriesCalculator.calculate(history(point(0, "0"), point(100, "11")), grid, null).changeCovered)
    }

    @Test
    fun `empty history with a live point yields a single uncovered point`() {
        val series = BittensorPortfolioValueSeriesCalculator.calculate(history(), emptyList(), live(1, "15", "4"))
        assertEquals(listOf(BigDecimal("60")), series.points.map { it.fiatValue })
        assertFalse(series.changeCovered)
    }

    @Test
    fun `zero and negative grid rates are ignored`() {
        val series = BittensorPortfolioValueSeriesCalculator.calculate(history(point(0, "10")), listOf(rate(0.0, "0"), rate(1.0, "3")), null)
        assertEquals(listOf(BigDecimal("30")), series.points.map { it.fiatValue })
    }
}
