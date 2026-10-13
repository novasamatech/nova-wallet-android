package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

class BittensorPortfolioHistoryBindingTest {

    private val period = BittensorPortfolioHistoryPeriod.SEVEN_DAYS

    private fun point(timestamp: String? = "2026-09-02T13:00:00Z", tao: String? = "12.5", usd: String? = "4000.1", completed: Boolean? = true) =
        BittensorPortfolioHistoryRemote.Point(timestamp, tao, usd, completed)

    private fun remote(
        period: String? = "SEVEN_DAYS",
        window: BittensorPortfolioHistoryRemote.Window? = BittensorPortfolioHistoryRemote.Window("2026-08-26T14:00:00Z", "2026-09-02T14:00:00Z"),
        points: List<BittensorPortfolioHistoryRemote.Point?>? = listOf(point(), point("2026-09-02T14:00:00Z", "13", "4100", completed = false)),
        historyScope: String? = "TAO_APP_PARTIAL",
        completeness: String? = "COMPLETE"
    ) = BittensorPortfolioHistoryRemote(period, window, points, historyScope, BittensorPortfolioHistoryRemote.Meta(completeness))

    private fun instant(epochSeconds: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(epochSeconds * 1000))

    private fun rejected(remote: BittensorPortfolioHistoryRemote) {
        try {
            bindBittensorPortfolioHistory(remote, period)
            fail("Expected the body to be rejected")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `valid body maps window, points and completeness`() {
        val history = bindBittensorPortfolioHistory(remote(), period)
        assertEquals(period, history.period)
        assertEquals(1787752800L, history.windowStart)
        assertEquals(1788357600L, history.windowEnd)
        assertEquals(2, history.points.size)
        assertEquals(1788354000L, history.points[0].timestamp)
        assertEquals(BigDecimal("12.5"), history.points[0].taoValue)
        assertEquals(BigDecimal("4000.1"), history.points[0].usdValue)
        assertTrue(history.points[0].completed)
        assertFalse(history.points[1].completed)
    }

    @Test
    fun `empty points is a valid empty series`() {
        assertEquals(emptyList<Any>(), bindBittensorPortfolioHistory(remote(points = emptyList()), period).points)
        assertEquals(emptyList<Any>(), bindBittensorPortfolioHistory(remote(points = null), period).points)
    }

    @Test
    fun `instant parsing accepts fractions and rejects offsets and garbage`() {
        assertEquals(1788354000L, parseBittensorInstant("2026-09-02T13:00:00Z"))
        assertEquals(1788354000L, parseBittensorInstant("2026-09-02T13:00:00.250Z"))
        assertNull(parseBittensorInstant("2026-09-02T13:00:00+02:00"))
        assertNull(parseBittensorInstant("2026-09-02 13:00:00Z"))
        assertNull(parseBittensorInstant("2026-13-02T13:00:00Z"))
        assertNull(parseBittensorInstant(null))
    }

    @Test
    fun `period echo must match the request`() = rejected(remote(period = "ONE_DAY"))

    @Test
    fun `history scope must be tao app partial`() = rejected(remote(historyScope = "FULL"))

    @Test
    fun `completeness must be complete`() = rejected(remote(completeness = "PARTIAL"))

    @Test
    fun `missing meta is rejected`() = rejected(BittensorPortfolioHistoryRemote("SEVEN_DAYS", remote().window, emptyList(), "TAO_APP_PARTIAL", null))

    @Test
    fun `more than 512 points is rejected`() {
        val windowStart = 1787752800L // 2026-08-26T14:00:00Z
        rejected(remote(points = (0 until 513).map { point(instant(windowStart + it * 60L)) }))
    }

    @Test
    fun `window must parse and be ordered`() {
        rejected(remote(window = null))
        rejected(remote(window = BittensorPortfolioHistoryRemote.Window("bad", "2026-09-02T14:00:00Z")))
        rejected(remote(window = BittensorPortfolioHistoryRemote.Window("2026-09-02T14:00:00Z", "2026-08-26T14:00:00Z")))
    }

    @Test
    fun `point outside the window is rejected`() = rejected(remote(points = listOf(point("2026-08-26T13:59:59Z"))))

    @Test
    fun `points must be strictly ascending`() = rejected(remote(points = listOf(point(), point())))

    @Test
    fun `unparseable timestamp is rejected`() = rejected(remote(points = listOf(point(timestamp = "yesterday"))))

    @Test
    fun `non decimal value is rejected`() {
        rejected(remote(points = listOf(point(tao = "1e3"))))
        rejected(remote(points = listOf(point(usd = null))))
        rejected(remote(points = listOf(point(tao = "NaN"))))
    }

    @Test
    fun `null point element is rejected like any other violation`() = rejected(remote(points = listOf(point(), null)))

    @Test
    fun `missing completed flag is rejected`() = rejected(remote(points = listOf(point(completed = null))))
}
