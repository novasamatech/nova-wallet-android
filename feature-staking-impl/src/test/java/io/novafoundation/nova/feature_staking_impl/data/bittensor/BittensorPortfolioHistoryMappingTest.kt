package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioHistoryMappingTest {

    private val period = BittensorPortfolioPeriod.THIRTY_DAYS

    @Test
    fun `typed DTO roundtrip preserves signed decimal completed and stale metadata`() {
        val original = portfolioHistoryResponse(points = listOf(point("-1.25")), freshness = "STALE")
        val gson = Gson()
        val decoded = gson.fromJson(gson.toJson(original), BittensorPortfolioHistoryRemote::class.java)
        val result = mapBittensorPortfolioHistory(decoded, period)
        assertEquals("-1.25", result.points.single().tao.toPlainString())
        assertTrue(result.points.single().completed)
        assertTrue(result.status.stale)
    }

    @Test
    fun `valid empty is accepted fresh or stale`() {
        assertTrue(mapBittensorPortfolioHistory(portfolioHistoryResponse(), period).points.isEmpty())
        assertFalse(mapBittensorPortfolioHistory(portfolioHistoryResponse(), period).status.stale)
        assertTrue(mapBittensorPortfolioHistory(portfolioHistoryResponse(freshness = "STALE"), period).status.stale)
    }

    @Test
    fun `request period scope window and metadata are validated`() {
        listOf(
            portfolioHistoryResponse(period = "ONE_DAY"),
            portfolioHistoryResponse(scope = "COMPLETE"),
            portfolioHistoryResponse(start = "2026-10-02T00:00:00Z"),
            portfolioHistoryResponse(freshness = "UNKNOWN"),
            portfolioHistoryResponse(start = "2026-02-30T00:00:00Z")
        ).forEach(::reject)
        val valid = portfolioHistoryResponse()
        reject(BittensorPortfolioHistoryRemote(valid.period, valid.window, null, valid.historyScope, valid.meta))
        reject(BittensorPortfolioHistoryRemote(valid.period, valid.window, emptyList(), valid.historyScope, null))
    }

    @Test
    fun `timestamps are bounded ascending and limited to 512`() {
        reject(portfolioHistoryResponse(points = listOf(point("1"), point("2"))))
        reject(portfolioHistoryResponse(points = listOf(point("1", time = "2026-08-31T23:59:59Z"))))
        reject(portfolioHistoryResponse(points = listOf(point("1", time = "2026-10-01T00:00:01Z"))))
        reject(portfolioHistoryResponse(points = List(513) { point("1") }))
        val endpoints = listOf(point("1", time = "2026-09-01T00:00:00Z"), point("2", time = "2026-10-01T00:00:00Z"))
        assertEquals(2, mapBittensorPortfolioHistory(portfolioHistoryResponse(points = endpoints), period).points.size)
        reject(portfolioHistoryResponse(points = endpoints.reversed()))
    }

    @Test
    fun `reported decimal budget rejects exponent whitespace malformed and missing fields`() {
        listOf("1e2", "NaN", "+1", "01", ".1", "1.", " 1", "1".repeat(1025)).forEach { value ->
            reject(portfolioHistoryResponse(points = listOf(point(value))))
        }
        assertEquals(
            1024,
            mapBittensorPortfolioHistory(portfolioHistoryResponse(points = listOf(point("1".repeat(1024)))), period)
                .points.single().tao.toPlainString().length
        )
        val incompleteDto = BittensorPortfolioHistoryRemote.Point("2026-09-02T00:00:00Z", "1", "2", null)
        reject(portfolioHistoryResponse(points = listOf(incompleteDto)))
    }

    private fun point(tao: String, time: String = "2026-09-02T00:00:00Z") =
        BittensorPortfolioHistoryRemote.Point(time, tao, "400", true)

    private fun reject(remote: BittensorPortfolioHistoryRemote) {
        assertTrue(runCatching { mapBittensorPortfolioHistory(remote, period) }.isFailure)
    }
}
