package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.formatting.parseDateISO_8601
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRemote
import io.novafoundation.nova.feature_wallet_api.domain.model.HistoricalCoinRate
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioRowInteractorTest {

    @Test
    fun `root APY is stake weighted and missing rates are not zero`() {
        val first = BittensorRootPosition(AccountIdKey(ByteArray(32)), BigInteger.ONE)
        val second = first.copy(stakeInPlanks = BigInteger.valueOf(3))
        val positions = listOf(first, second)
        assertEquals(0, requireNotNull(weightedRootApy(positions, listOf(BigDecimal.TEN, BigDecimal.valueOf(30)))).compareTo(BigDecimal.valueOf(25)))
        assertEquals(0, requireNotNull(weightedRootApy(positions, listOf(BigDecimal.ZERO, BigDecimal.ZERO))).compareTo(BigDecimal.ZERO))
        assertNull(weightedRootApy(positions, listOf(BigDecimal.TEN, null)))
        assertNull(weightedRootApy(emptyList(), emptyList()))
    }

    @Test
    fun `weekly USD change is normalized by native token change`() {
        val market = market(BigDecimal.valueOf(20))
        val history = listOf(HistoricalCoinRate(0, BigDecimal.TEN), HistoricalCoinRate(WEEK_SECONDS, BigDecimal.valueOf(12)))
        assertEquals(0, requireNotNull(weeklyChangeInTao(market, history, NOW_MILLIS)).compareTo(BigDecimal.ZERO))
        val flatNative = listOf(HistoricalCoinRate(0, BigDecimal.TEN), HistoricalCoinRate(WEEK_SECONDS, BigDecimal.TEN))
        assertEquals(0, requireNotNull(weeklyChangeInTao(market, flatNative, NOW_MILLIS)).compareTo(BigDecimal.valueOf(20)))
        assertEquals(0, requireNotNull(weeklyChangeInTao(market(BigDecimal.valueOf(-10)), flatNative, NOW_MILLIS)).compareTo(BigDecimal.valueOf(-10)))
    }

    @Test
    fun `missing stale and short weekly data are hidden rather than zero`() {
        val history = listOf(HistoricalCoinRate(0, BigDecimal.ONE), HistoricalCoinRate(WEEK_SECONDS, BigDecimal.ONE))
        assertNull(weeklyChangeInTao(market(null), history, NOW_MILLIS))
        assertNull(weeklyChangeInTao(market(BigDecimal.ONE).copy(lastUpdated = null), history, NOW_MILLIS))
        assertNull(weeklyChangeInTao(market(BigDecimal.ONE), history, NOW_MILLIS + 86_400_001))
        assertNull(weeklyChangeInTao(market(BigDecimal.ONE), history.take(1), NOW_MILLIS))
    }

    @Test
    fun `sparkline uses native token ratios rather than USD prices`() {
        val history = listOf(HistoricalCoinRate(0, BigDecimal.TEN), HistoricalCoinRate(WEEK_SECONDS, BigDecimal.valueOf(20)))
        val points = weeklySparklineInTao(listOf(BigDecimal.valueOf(20), BigDecimal.valueOf(60)), history)

        assertEquals(listOf(BigDecimal.valueOf(2), BigDecimal.valueOf(3)), points.map { it.stripTrailingZeros() })
    }

    @Test
    fun `sparkline does not match observations outside the native sampling tolerance`() {
        val history = listOf(HistoricalCoinRate(WEEK_SECONDS, BigDecimal.TEN))
        assertEquals(emptyList<BigDecimal>(), weeklySparklineInTao(listOf(BigDecimal.TEN, BigDecimal.TEN), history))
        assertEquals(emptyList<BigDecimal>(), weeklySparklineInTao(listOf(null, BigDecimal.TEN), history))
    }

    @Test
    fun `sparkline has at most 48 real observations`() {
        val history = (0..168).map { HistoricalCoinRate(WEEK_SECONDS * it / 168, BigDecimal.TEN) }
        val prices = (0..168).map { (it + 1).toBigDecimal() }
        val points = weeklySparklineInTao(prices, history)
        assertTrue(points.size in 2..48)
        assertEquals(0, prices.last().divide(BigDecimal.TEN).compareTo(points.last()))
    }

    private fun market(change: BigDecimal?) = BittensorSubnetMarketRemote("subnet", "sn1", change, "2026-10-19T00:00:00.000Z")

    private companion object {
        const val WEEK_SECONDS = 604_800L
        val NOW_MILLIS = requireNotNull(parseDateISO_8601("2026-10-19T00:00:00.000Z")).time
    }
}
