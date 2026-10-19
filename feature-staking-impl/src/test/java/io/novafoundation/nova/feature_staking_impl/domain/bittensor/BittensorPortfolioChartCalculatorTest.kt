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
        assertEquals(listOf(2000L, 10000L), calculate(positions, missing, BigDecimal("4"), 9, 10000).points.map { it.timestamp })
    }

    @Test
    fun `empty FX is not reported USD and only permits a live anchor`() {
        val history = history(listOf(point(1000, "12", "4800"))).copy(nativePrices = emptyList())
        val result = calculate(positions, history, BigDecimal("4"), 9, 10000)
        assertEquals(1, result.points.size)
        assertDecimal("8", result.points.single().fiat)
        assertNull(result.change)
    }

    @Test
    fun `coverage uses server window and backend sampling rather than FX tolerance`() {
        BittensorPortfolioPeriod.entries.forEach { period ->
            val boundary = 2 * period.sampleStepSeconds
            val covered = history(listOf(point(boundary, "1", "10")), period, 0)
            val uncovered = history(listOf(point(boundary + 1, "1", "10")), period, 0)
            assertTrue(calculate(positions, covered, BigDecimal.TEN, 9, boundary + 10000).change != null)
            assertNull(calculate(positions, uncovered, BigDecimal.TEN, 9, boundary + 10000).change)
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
        assertEquals(listOf(1000L), result.points.map { it.timestamp })
    }

    @Test
    fun `do not append live point before the newest completed point`() {
        val result = calculate(positions, history(listOf(point(1000, "1", "10"))), BigDecimal.TEN, 9, 1000)
        assertEquals(1, result.points.size)
        assertDecimal("1", result.points.single().tao)
    }

    @Test
    fun `missing current price preserves history without a fake zero anchor`() {
        val history = history(listOf(point(1000, "12", "4800")))
        val anchor = BittensorPortfolioLiveAnchor(BigDecimal.TEN, null)
        val result = calculator.calculate(history, anchor, 10000)
        assertEquals(listOf(1000L), result.points.map { it.timestamp })
        assertDecimal("4800", result.points.single().fiat)
    }

    @Test
    fun `missing holdings preserves completed account history`() {
        val history = history(listOf(point(1000, "12", "4800")))
        val result = calculator.calculate(history, null, 10000)
        assertDecimal("12", result.points.single().tao)
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
        start: Long = 1000
    ): BittensorPortfolioHistory {
        val status = BittensorPortfolioHistoryStatus(BigDecimal.ZERO, false)
        val account = BittensorAccountHistory(period, start, 1000000, points, status)
        return BittensorPortfolioHistory(account, null)
    }

    private fun point(time: Long, tao: String, usd: String, completed: Boolean = true) =
        BittensorPortfolioHistoryPoint(time, tao.toBigDecimal(), usd.toBigDecimal(), completed)

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals(0, expected.toBigDecimal().compareTo(actual))
    }
}
