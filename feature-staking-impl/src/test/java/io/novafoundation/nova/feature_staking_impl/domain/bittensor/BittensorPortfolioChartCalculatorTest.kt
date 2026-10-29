package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioChartCalculatorTest {

    private val calculator = BittensorPortfolioChartCalculator()

    private val root = BittensorRootPosition(
        validatorHotkey = AccountIdKey(ByteArray(size = 32)),
        stakeInPlanks = BigInteger.TEN.pow(9) * BigInteger.TWO
    )

    private val positions = BittensorPortfolioPositions(root = listOf(root), subnets = emptyList(), subnetPrices = emptyMap())

    @Test
    fun `historical amounts are independent of current holdings`() {
        val history = history(listOf(point(time = 1000, tao = "12", usd = "4800")))
        val first = calculate(positions, history, BigDecimal.TEN, precision = 9, now = 10000)
        val changed = positions.copy(root = listOf(root.copy(stakeInPlanks = BigInteger.ZERO)))
        val second = calculate(changed, history, BigDecimal.TEN, precision = 9, now = 10000)

        assertEquals(first.points.first(), second.points.first())
        assertDecimal(expected = "12", second.points.first().tao)
        assertDecimal(expected = "4800", second.points.first().fiat)
        assertDecimal(expected = "0", second.points.last().tao)
        assertDecimal(expected = "0", second.points.last().fiat)
    }

    @Test
    fun `open bucket supplies live USD ratio but is not a historical point`() {
        val history = history(
            listOf(point(time = 1000, tao = "12.5", usd = "4875.25"), point(time = 2000, tao = "13.4", usd = "5400.2", completed = false))
        )
        val result = calculate(positions, history, currentPrice = BigDecimal("410"), precision = 9, now = 2100)

        assertEquals(listOf(1000L, 2100L), result.points.map { it.timestamp })
        assertDecimal(expected = "806", result.points.last().fiat)
        assertDecimal(expected = "2", result.points.last().tao)
    }

    @Test
    fun `expired reported USD ratio falls back to current price`() {
        val history = history(listOf(point(time = 1000, tao = "1", usd = "400")))
        val result = calculate(positions, history, currentPrice = BigDecimal("410"), precision = 9, now = 10000)
        assertDecimal(expected = "820", result.points.last().fiat)
    }

    @Test
    fun `non USD uses nearest positive FX within period tolerance`() {
        val history = history(
            points = listOf(point(time = 1000, tao = "12", usd = "4800"), point(time = 2000, tao = "13", usd = "5200")),
            fiatHistory = BittensorPortfolioFiatHistory.Converted(
                nativePrices = listOf(
                    HistoricalCoinRate(timestamp = 1600, rate = BigDecimal("3")),
                    HistoricalCoinRate(timestamp = 2000, rate = BigDecimal.ZERO)
                )
            )
        )
        val result = calculate(positions, history, currentPrice = BigDecimal("4"), precision = 9, now = 10000)

        assertDecimal(expected = "36", result.points[0].fiat)
        assertDecimal(expected = "39", result.points[1].fiat)
        assertDecimal(expected = "8", result.points.last().fiat)
        val missing = history.copy(
            fiatHistory = BittensorPortfolioFiatHistory.Converted(
                nativePrices = listOf(HistoricalCoinRate(timestamp = 1601, rate = BigDecimal("3")))
            )
        )
        val missingPoints = calculate(positions, missing, currentPrice = BigDecimal("4"), precision = 9, now = 10000).points
        // The unpriced 1000 point is skipped, so the window starts at zero; 2000 is taken by the 4600 instant.
        assertEquals(listOf(1000L, 2000L, 8200L, 10000L), missingPoints.map { it.timestamp })
        assertDecimal(expected = "0", missingPoints[0].fiat)
        assertDecimal(expected = "39", missingPoints[2].fiat)
    }

    @Test
    fun `empty FX is not reported USD and only permits a live anchor`() {
        val history = history(
            points = listOf(point(time = 1000, tao = "12", usd = "4800")),
            fiatHistory = BittensorPortfolioFiatHistory.Converted(nativePrices = emptyList())
        )
        val result = calculate(positions, history, currentPrice = BigDecimal("4"), precision = 9, now = 10000)
        assertEquals(listOf(1000L, 4600L, 8200L, 10000L), result.points.map { it.timestamp })
        assertTrue(result.points.dropLast(1).all { it.tao.isZero && it.fiat.isZero })
        assertDecimal(expected = "8", result.points.last().fiat)
        assertNull(result.change)
    }

    @Test
    fun `change is shown only when the window starts with a value`() {
        BittensorPortfolioPeriod.entries.forEach { period ->
            val step = period.sampleStepSeconds
            val fromStart = history(listOf(point(time = 0, tao = "1", usd = "10")), period, start = 0)
            val afterStart = history(listOf(point(step, tao = "1", usd = "10")), period, start = 0)
            assertDecimal(expected = "1", calculate(positions, fromStart, BigDecimal.TEN, precision = 9, now = step + 10000).change!!)
            assertNull(calculate(positions, afterStart, BigDecimal.TEN, precision = 9, now = step + 10000).change)
        }
    }

    @Test
    fun `zero first value and one point do not fabricate return`() {
        val zeroHistory = history(listOf(point(time = 1000, tao = "0", usd = "0")))
        val emptyHistory = history(points = emptyList())
        assertNull(calculate(positions, zeroHistory, BigDecimal.TEN, precision = 9, now = 10000).change)
        assertNull(calculate(positions, emptyHistory, BigDecimal.TEN, precision = 9, now = 10000).change)
    }

    @Test
    fun `unknown current valuation cannot append a fabricated live point`() {
        val subnetId = BittensorNetuid.Subnet(value = 1)
        val subnet = BittensorSubnetPosition(subnetId, root.validatorHotkey, BigInteger.ONE)
        val unpriced = positions.copy(subnets = listOf(subnet))
        val history = history(listOf(point(time = 1000, tao = "12", usd = "4800")))
        val result = calculate(unpriced, history, BigDecimal.TEN, precision = 9, now = 10000)
        assertEquals(listOf(1000L, 4600L, 8200L), result.points.map { it.timestamp })
        assertTrue(result.points.all { it.tao.compareTo(BigDecimal("12")) == 0 })
    }

    @Test
    fun `grid instants at or after now are left to the live point`() {
        val history = history(listOf(point(time = 1000, tao = "1", usd = "10")))
        val result = calculate(positions, history, BigDecimal.TEN, precision = 9, now = 1000)
        assertEquals(listOf(1000L), result.points.map { it.timestamp })
        assertDecimal(expected = "2", result.points.single().tao)
    }

    @Test
    fun `missing current price preserves history without a fake zero anchor`() {
        val history = history(listOf(point(time = 1000, tao = "12", usd = "4800")))
        val anchor = BittensorPortfolioLiveAnchor(tao = BigDecimal.TEN, currentNativePrice = null)
        val result = calculator.calculate(history, anchor, now = 10000)
        assertEquals(listOf(1000L, 4600L, 8200L), result.points.map { it.timestamp })
        assertTrue(result.points.all { it.fiat.compareTo(BigDecimal("4800")) == 0 })
    }

    @Test
    fun `missing holdings preserves completed account history`() {
        val history = history(listOf(point(time = 1000, tao = "12", usd = "4800")))
        val result = calculator.calculate(history, liveAnchor = null, now = 10000)
        assertEquals(3, result.points.size)
        assertTrue(result.points.all { it.tao.compareTo(BigDecimal("12")) == 0 })
    }

    @Test
    fun `stale recent bucket cannot supply live USD price`() {
        val fresh = history(listOf(point(time = 1000, tao = "1", usd = "400")))
        val staleAccount = fresh.account.copy(status = fresh.account.status.copy(stale = true))
        val stale = fresh.copy(account = staleAccount)
        val anchor = BittensorPortfolioLiveAnchor(tao = BigDecimal("2"), currentNativePrice = BigDecimal("410"))
        val result = calculator.calculate(stale, anchor, now = 1100)
        assertDecimal(expected = "400", result.points.first().fiat)
        assertDecimal(expected = "820", result.points.last().fiat)
        val missingPrice = calculator.calculate(stale, anchor.copy(currentNativePrice = null), now = 1100)
        assertEquals(listOf(1000L), missingPrice.points.map { it.timestamp })
    }

    @Test
    fun `thirty day history with only the open bucket fills the window with zeros then the live point`() {
        val start = 1_000_000L
        val end = start + 30 * 24 * 3600
        val open = point(end - 3600, tao = "0", usd = "0", completed = false)
        val history = history(listOf(open), BittensorPortfolioPeriod.THIRTY_DAYS, start, end)
        val result = calculate(positions, history, currentPrice = BigDecimal("273"), precision = 9, now = end - 2400)

        // 180 four-hour instants before now plus the live point
        assertEquals(181, result.points.size)
        assertEquals(start, result.points.first().timestamp)
        assertTrue(result.points.dropLast(1).all { it.tao.isZero && it.fiat.isZero })
        assertEquals(end - 2400, result.points.last().timestamp)
        assertDecimal(expected = "2", result.points.last().tao)
        assertDecimal(expected = "546", result.points.last().fiat)
        assertNull(result.change)
    }

    @Test
    fun `one day history with a single completed point starts at zero and jumps`() {
        val start = 1_000_000L
        val end = start + 24 * 3600
        val stake = point(end - 2 * 3600, tao = "12.44", usd = "3397.51")
        val history = history(
            listOf(stake, point(end - 3600, tao = "0", usd = "0", completed = false)),
            BittensorPortfolioPeriod.ONE_DAY,
            start,
            end
        )
        val result = calculate(positions, history, currentPrice = BigDecimal("273"), precision = 9, now = end - 2400)

        // 22 zero hours, the completed point, its carried copy at the next hour, then live
        assertEquals(25, result.points.size)
        assertTrue(result.points.take(22).all { it.tao.isZero && it.fiat.isZero })
        assertEquals(stake.timestamp, result.points[22].timestamp)
        assertDecimal(expected = "12.44", result.points[22].tao)
        assertDecimal(expected = "3397.51", result.points[22].fiat)
        assertEquals(end - 3600, result.points[23].timestamp)
        assertDecimal(expected = "3397.51", result.points[23].fiat)
        assertDecimal(expected = "2", result.points[24].tao)
        assertNull(result.change)
    }

    @Test
    fun `empty ninety day history fills the window with zeros then the live point`() {
        val start = 1_000_000L
        val end = start + 90 * 24 * 3600
        val history = history(points = emptyList(), BittensorPortfolioPeriod.NINETY_DAYS, start, end)
        val result = calculate(positions, history, currentPrice = BigDecimal("273"), precision = 9, now = end - 2400)

        assertEquals(271, result.points.size)
        assertTrue(result.points.dropLast(1).all { it.tao.isZero && it.fiat.isZero })
        assertDecimal(expected = "2", result.points.last().tao)
        assertNull(result.change)
    }

    @Test
    fun `gap inside the series carries the last value and reprices it with FX history`() {
        val start = 1_000_000L
        val end = start + 24 * 3600
        val fx = (0 until 24).map { HistoricalCoinRate(timestamp = start + it * 3600L, rate = BigDecimal(270 + it)) }
        val history = history(
            listOf(point(start, tao = "10", usd = "2700")),
            BittensorPortfolioPeriod.ONE_DAY,
            start,
            end,
            fiatHistory = BittensorPortfolioFiatHistory.Converted(fx)
        )
        val result = calculate(positions, history, currentPrice = BigDecimal("280"), precision = 9, now = start + 3 * 3600 + 1800)

        // the real point, three carried hours at the hourly TAO price, then live at 280
        assertEquals(listOf(start, start + 3600, start + 7200, start + 10800, start + 12600), result.points.map { it.timestamp })
        assertDecimal(expected = "2700", result.points[0].fiat)
        assertDecimal(expected = "2710", result.points[1].fiat)
        assertDecimal(expected = "2730", result.points[3].fiat)
        assertDecimal(expected = "560", result.points[4].fiat)
    }

    @Test
    fun `only reported USD uses a recent bucket ratio for the live anchor`() {
        val reported = history(listOf(point(time = 1000, tao = "1", usd = "400")))
        val anchor = BittensorPortfolioLiveAnchor(tao = BigDecimal("2"), currentNativePrice = BigDecimal("4"))
        val reportedResult = calculator.calculate(reported, anchor, now = 1100)
        assertDecimal(expected = "400", reportedResult.points.first().fiat)
        assertDecimal(expected = "800", reportedResult.points.last().fiat)

        val converted = reported.copy(
            fiatHistory = BittensorPortfolioFiatHistory.Converted(
                nativePrices = listOf(HistoricalCoinRate(timestamp = 1000, rate = BigDecimal("3")))
            )
        )
        val convertedResult = calculator.calculate(converted, anchor, now = 1100)
        assertDecimal(expected = "3", convertedResult.points.first().fiat)
        assertDecimal(expected = "8", convertedResult.points.last().fiat)

        val emptyConverted = reported.copy(fiatHistory = BittensorPortfolioFiatHistory.Converted(nativePrices = emptyList()))
        val emptyResult = calculator.calculate(emptyConverted, anchor, now = 1100)
        assertTrue(emptyResult.points.first().fiat.isZero)
        assertDecimal(expected = "8", emptyResult.points.last().fiat)

        val missingPrice = anchor.copy(currentNativePrice = null)
        assertDecimal(expected = "800", calculator.calculate(reported, missingPrice, now = 1100).points.last().fiat)
        assertEquals(1, calculator.calculate(converted, missingPrice, now = 1100).points.size)
        assertEquals(1, calculator.calculate(emptyConverted, missingPrice, now = 1100).points.size)
    }

    @Test
    fun `converted history respects the inclusive FX tolerance of every period`() {
        val start = 1_000_000L
        BittensorPortfolioPeriod.entries.forEach { period ->
            val atBoundary = history(
                points = listOf(point(start, tao = "2", usd = "800")),
                period = period,
                start = start,
                end = start + period.sampleStepSeconds,
                fiatHistory = BittensorPortfolioFiatHistory.Converted(
                    nativePrices = listOf(HistoricalCoinRate(timestamp = start + period.fxToleranceSeconds, rate = BigDecimal("3")))
                )
            )
            val now = atBoundary.account.windowEnd + 1
            val included = calculator.calculate(atBoundary, liveAnchor = null, now)
            assertDecimal(expected = "2", included.points.first().tao)
            assertDecimal(expected = "6", included.points.first().fiat)

            val outsideBoundary = atBoundary.copy(
                fiatHistory = BittensorPortfolioFiatHistory.Converted(
                    nativePrices = listOf(HistoricalCoinRate(timestamp = start + period.fxToleranceSeconds + 1, rate = BigDecimal("3")))
                )
            )
            val skipped = calculator.calculate(outsideBoundary, liveAnchor = null, now)
            assertEquals(listOf(start, atBoundary.account.windowEnd), skipped.points.map { it.timestamp })
            assertTrue(skipped.points.all { it.tao.isZero && it.fiat.isZero })
            assertNull(skipped.change)
        }
    }

    @Test
    fun `converted history sorts usable prices and retains the last fiat when a gap has no FX`() {
        val history = history(
            points = listOf(point(time = 4600, tao = "2", usd = "800"), point(time = 1000, tao = "3", usd = "1200")),
            end = 8200,
            fiatHistory = BittensorPortfolioFiatHistory.Converted(
                nativePrices = listOf(
                    HistoricalCoinRate(timestamp = 8200, rate = BigDecimal.ZERO),
                    HistoricalCoinRate(timestamp = 4600, rate = BigDecimal("4")),
                    HistoricalCoinRate(timestamp = 1000, rate = BigDecimal("-1")),
                    HistoricalCoinRate(timestamp = 1000, rate = BigDecimal("3"))
                )
            )
        )
        val result = calculator.calculate(history, liveAnchor = null, now = 10000)
        assertEquals(listOf(1000L, 4600L, 8200L), result.points.map { it.timestamp })
        assertDecimal(expected = "9", result.points[0].fiat)
        assertDecimal(expected = "8", result.points[1].fiat)
        assertDecimal(expected = "2", result.points[2].tao)
        assertDecimal(expected = "8", result.points[2].fiat)
    }

    @Test
    fun `reported USD live ratios retain scale eighteen and half even rounding`() {
        val anchor = BittensorPortfolioLiveAnchor(tao = BigDecimal.ONE, currentNativePrice = null)
        val roundDown = history(listOf(point(time = 1000, tao = "1", usd = "1.0000000000000000005")))
        val roundUp = history(listOf(point(time = 1000, tao = "1", usd = "1.0000000000000000015")))

        assertEquals(BigDecimal("1.000000000000000000"), calculator.calculate(roundDown, anchor, now = 1100).points.last().fiat)
        assertEquals(BigDecimal("1.000000000000000002"), calculator.calculate(roundUp, anchor, now = 1100).points.last().fiat)
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
        end: Long = 1000000,
        fiatHistory: BittensorPortfolioFiatHistory = BittensorPortfolioFiatHistory.ReportedUsd
    ): BittensorPortfolioHistory {
        val status = BittensorPortfolioHistoryStatus(asOf = BigDecimal.ZERO, stale = false)
        val account = BittensorAccountHistory(period, start, end, points, status)
        return BittensorPortfolioHistory(account, fiatHistory)
    }

    private fun point(time: Long, tao: String, usd: String, completed: Boolean = true) =
        BittensorPortfolioHistoryPoint(time, tao.toBigDecimal(), usd.toBigDecimal(), completed)

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals(0, expected.toBigDecimal().compareTo(actual))
    }
}
