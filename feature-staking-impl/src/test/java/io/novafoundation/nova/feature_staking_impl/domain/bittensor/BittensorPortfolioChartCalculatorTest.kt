package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioChartCalculatorTest {

    private val calculator = BittensorPortfolioChartCalculator()

    private val root = BittensorRootPosition(AccountIdKey(ByteArray(32)), BigInteger.TEN.pow(9) * BigInteger.TWO)

    private val positions = BittensorPortfolioPositions(listOf(root), emptyList(), emptyMap())

    @Test
    fun `historical amounts are independent of current holdings`() {
        val history = history(listOf(point(1000, "12", "4800")))
        val first = calculate(positions, history, BigDecimal.TEN, 9, 10000)
        val changed = positions.copy(root = listOf(root.copy(stakeInPlanks = BigInteger.ZERO)))
        val second = calculate(changed, history, BigDecimal.TEN, 9, 10000)

        assertEquals(first.points.first(), second.points.first())
        assertDecimal("12", second.points.first().tao)
        assertDecimal("4800", second.points.first().fiat)
        assertDecimal("0", second.points.last().tao)
        assertDecimal("0", second.points.last().fiat)
    }

    @Test
    fun `open bucket supplies live USD ratio but is not a historical point`() {
        val history = history(listOf(point(1000, "12.5", "4875.25"), point(2000, "13.4", "5400.2", false)))
        val result = calculate(positions, history, BigDecimal("410"), 9, 2100)

        assertEquals(listOf(1000L, 2100L), result.points.map { it.timestamp })
        assertDecimal("806", result.points.last().fiat)
        assertDecimal("2", result.points.last().tao)
    }

    @Test
    fun `expired reported USD ratio falls back to current price`() {
        val result = calculate(positions, history(listOf(point(1000, "1", "400"))), BigDecimal("410"), 9, 10000)
        assertDecimal("820", result.points.last().fiat)
    }

    @Test
    fun `non USD uses nearest positive FX within period tolerance`() {
        val history = history(listOf(point(1000, "12", "4800"), point(2000, "13", "5200")))
            .copy(nativePrices = listOf(HistoricalCoinRate(1600, BigDecimal("3")), HistoricalCoinRate(2000, BigDecimal.ZERO)))
        val result = calculate(positions, history, BigDecimal("4"), 9, 10000)

        assertDecimal("36", result.points[0].fiat)
        assertDecimal("39", result.points[1].fiat)
        assertDecimal("8", result.points.last().fiat)
        val missing = history.copy(nativePrices = listOf(HistoricalCoinRate(1601, BigDecimal("3"))))
        val missingPoints = calculate(positions, missing, BigDecimal("4"), 9, 10000).points
        // The unpriced 1000 point is skipped, so the window starts at zero; 2000 is taken by the 4600 instant.
        assertEquals(listOf(1000L, 2000L, 8200L, 10000L), missingPoints.map { it.timestamp })
        assertDecimal("0", missingPoints[0].fiat)
        assertDecimal("39", missingPoints[2].fiat)
    }

    @Test
    fun `empty FX is not reported USD and only permits a live anchor`() {
        val history = history(listOf(point(1000, "12", "4800"))).copy(nativePrices = emptyList())
        val result = calculate(positions, history, BigDecimal("4"), 9, 10000)
        assertEquals(listOf(1000L, 4600L, 8200L, 10000L), result.points.map { it.timestamp })
        assertTrue(result.points.dropLast(1).all { it.tao.signum() == 0 && it.fiat.signum() == 0 })
        assertDecimal("8", result.points.last().fiat)
        assertNull(result.change)
    }

    @Test
    fun `change is shown only when the window starts with a value`() {
        BittensorPortfolioPeriod.entries.forEach { period ->
            val step = period.sampleStepSeconds
            val fromStart = history(listOf(point(0, "1", "10")), period, 0)
            val afterStart = history(listOf(point(step, "1", "10")), period, 0)
            assertDecimal("1", calculate(positions, fromStart, BigDecimal.TEN, 9, step + 10000).change!!)
            assertNull(calculate(positions, afterStart, BigDecimal.TEN, 9, step + 10000).change)
        }
    }

    @Test
    fun `zero first value and one point do not fabricate return`() {
        assertNull(calculate(positions, history(listOf(point(1000, "0", "0"))), BigDecimal.TEN, 9, 10000).change)
        assertNull(calculate(positions, history(emptyList()), BigDecimal.TEN, 9, 10000).change)
    }

    @Test
    fun `unknown current valuation cannot append a fabricated live point`() {
        val subnetId = BittensorNetuid.Subnet(1)
        val subnet = BittensorSubnetPosition(subnetId, root.validatorHotkey, BigInteger.ONE)
        val unpriced = positions.copy(subnets = listOf(subnet))
        val result = calculate(unpriced, history(listOf(point(1000, "12", "4800"))), BigDecimal.TEN, 9, 10000)
        assertEquals(listOf(1000L, 4600L, 8200L), result.points.map { it.timestamp })
        assertTrue(result.points.all { it.tao.compareTo(BigDecimal("12")) == 0 })
    }

    @Test
    fun `grid instants at or after now are left to the live point`() {
        val result = calculate(positions, history(listOf(point(1000, "1", "10"))), BigDecimal.TEN, 9, 1000)
        assertEquals(listOf(1000L), result.points.map { it.timestamp })
        assertDecimal("2", result.points.single().tao)
    }

    @Test
    fun `missing current price preserves history without a fake zero anchor`() {
        val history = history(listOf(point(1000, "12", "4800")))
        val anchor = BittensorPortfolioLiveAnchor(BigDecimal.TEN, null)
        val result = calculator.calculate(history, anchor, 10000)
        assertEquals(listOf(1000L, 4600L, 8200L), result.points.map { it.timestamp })
        assertTrue(result.points.all { it.fiat.compareTo(BigDecimal("4800")) == 0 })
    }

    @Test
    fun `missing holdings preserves completed account history`() {
        val history = history(listOf(point(1000, "12", "4800")))
        val result = calculator.calculate(history, null, 10000)
        assertEquals(3, result.points.size)
        assertTrue(result.points.all { it.tao.compareTo(BigDecimal("12")) == 0 })
    }

    @Test
    fun `stale recent bucket cannot supply live USD price`() {
        val fresh = history(listOf(point(1000, "1", "400")))
        val staleAccount = fresh.account.copy(status = fresh.account.status.copy(stale = true))
        val stale = fresh.copy(account = staleAccount)
        val anchor = BittensorPortfolioLiveAnchor(BigDecimal("2"), BigDecimal("410"))
        val result = calculator.calculate(stale, anchor, 1100)
        assertDecimal("400", result.points.first().fiat)
        assertDecimal("820", result.points.last().fiat)
        val missingPrice = calculator.calculate(stale, anchor.copy(currentNativePrice = null), 1100)
        assertEquals(listOf(1000L), missingPrice.points.map { it.timestamp })
    }

    @Test
    fun `thirty day history with only the open bucket fills the window with zeros then the live point`() {
        val start = 1_000_000L
        val end = start + 30 * 24 * 3600
        val open = point(end - 3600, "0", "0", completed = false)
        val history = history(listOf(open), BittensorPortfolioPeriod.THIRTY_DAYS, start, end)
        val result = calculate(positions, history, BigDecimal("273"), 9, end - 2400)

        // 180 four-hour instants before now plus the live point
        assertEquals(181, result.points.size)
        assertEquals(start, result.points.first().timestamp)
        assertTrue(result.points.dropLast(1).all { it.tao.signum() == 0 && it.fiat.signum() == 0 })
        assertEquals(end - 2400, result.points.last().timestamp)
        assertDecimal("2", result.points.last().tao)
        assertDecimal("546", result.points.last().fiat)
        assertNull(result.change)
    }

    @Test
    fun `one day history with a single completed point starts at zero and jumps`() {
        val start = 1_000_000L
        val end = start + 24 * 3600
        val stake = point(end - 2 * 3600, "12.44", "3397.51")
        val history = history(listOf(stake, point(end - 3600, "0", "0", completed = false)), BittensorPortfolioPeriod.ONE_DAY, start, end)
        val result = calculate(positions, history, BigDecimal("273"), 9, end - 2400)

        // 22 zero hours, the completed point, its carried copy at the next hour, then live
        assertEquals(25, result.points.size)
        assertTrue(result.points.take(22).all { it.tao.signum() == 0 && it.fiat.signum() == 0 })
        assertEquals(stake.timestamp, result.points[22].timestamp)
        assertDecimal("12.44", result.points[22].tao)
        assertDecimal("3397.51", result.points[22].fiat)
        assertEquals(end - 3600, result.points[23].timestamp)
        assertDecimal("3397.51", result.points[23].fiat)
        assertDecimal("2", result.points[24].tao)
        assertNull(result.change)
    }

    @Test
    fun `empty ninety day history fills the window with zeros then the live point`() {
        val start = 1_000_000L
        val end = start + 90 * 24 * 3600
        val history = history(emptyList(), BittensorPortfolioPeriod.NINETY_DAYS, start, end)
        val result = calculate(positions, history, BigDecimal("273"), 9, end - 2400)

        assertEquals(271, result.points.size)
        assertTrue(result.points.dropLast(1).all { it.tao.signum() == 0 && it.fiat.signum() == 0 })
        assertDecimal("2", result.points.last().tao)
        assertNull(result.change)
    }

    @Test
    fun `gap inside the series carries the last value and reprices it with FX history`() {
        val start = 1_000_000L
        val end = start + 24 * 3600
        val fx = (0 until 24).map { HistoricalCoinRate(start + it * 3600L, BigDecimal(270 + it)) }
        val history = history(listOf(point(start, "10", "2700")), BittensorPortfolioPeriod.ONE_DAY, start, end).copy(nativePrices = fx)
        val result = calculate(positions, history, BigDecimal("280"), 9, start + 3 * 3600 + 1800)

        // the real point, three carried hours at the hourly TAO price, then live at 280
        assertEquals(listOf(start, start + 3600, start + 7200, start + 10800, start + 12600), result.points.map { it.timestamp })
        assertDecimal("2700", result.points[0].fiat)
        assertDecimal("2710", result.points[1].fiat)
        assertDecimal("2730", result.points[3].fiat)
        assertDecimal("560", result.points[4].fiat)
    }

    private fun calculate(
        positions: BittensorPortfolioPositions,
        history: BittensorPortfolioHistory,
        currentPrice: BigDecimal,
        precision: Int,
        now: Long
    ): BittensorPortfolioValueSeries {
        val anchor = positions.totalStake?.let { BittensorPortfolioLiveAnchor(BigDecimal(it, precision), currentPrice) }
        return calculator.calculate(history, anchor, now)
    }

    private fun history(
        points: List<BittensorPortfolioHistoryPoint>,
        period: BittensorPortfolioPeriod = BittensorPortfolioPeriod.ONE_DAY,
        start: Long = 1000,
        end: Long = 1000000
    ): BittensorPortfolioHistory {
        val status = BittensorPortfolioHistoryStatus(BigDecimal.ZERO, false)
        val account = BittensorAccountHistory(period, start, end, points, status)
        return BittensorPortfolioHistory(account, null)
    }

    private fun point(time: Long, tao: String, usd: String, completed: Boolean = true) =
        BittensorPortfolioHistoryPoint(time, tao.toBigDecimal(), usd.toBigDecimal(), completed)

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals(0, expected.toBigDecimal().compareTo(actual))
    }
}
