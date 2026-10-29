package io.novafoundation.nova.feature_staking_impl.data.bittensor

import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioHistoryMappingTest {

    private val period = BittensorPortfolioPeriod.THIRTY_DAYS

    private val gson = Gson()

    @Test
    fun `typed DTO roundtrip preserves signed decimal completed and stale metadata`() {
        val original = portfolioHistoryResponse(points = listOf(point(tao = "-1.25", usd = "-400.50")), freshness = "STALE")
        val decoded = gson.fromJson(gson.toJson(original), BittensorPortfolioHistoryRemote::class.java)
        val result = mapBittensorPortfolioHistory(decoded, period)
        assertEquals("-1.25", result.points.single().tao.toPlainString())
        assertEquals("-400.50", result.points.single().usd.toPlainString())
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
        reject(BittensorPortfolioHistoryRemote(valid.period, valid.window, points = null, valid.historyScope, valid.meta))
        reject(BittensorPortfolioHistoryRemote(valid.period, valid.window, points = emptyList(), valid.historyScope, meta = null))
    }

    @Test
    fun `required metadata fields reject unknown and missing wire values`() {
        listOf("completeness", "availability", "valueQuality", "sourceClass", "asOf").forEach { field ->
            val json = gson.toJsonTree(portfolioHistoryResponse()).asJsonObject
            val metadata = json.getAsJsonObject("meta")
            val owner = if (field == "completeness") metadata else metadata.getAsJsonObject("components").getAsJsonObject("portfolioHistory")

            owner.addProperty(field, "UNKNOWN")
            reject(gson.fromJson(json, BittensorPortfolioHistoryRemote::class.java))
            owner.remove(field)
            reject(gson.fromJson(json, BittensorPortfolioHistoryRemote::class.java))
        }
    }

    @Test
    fun `timestamps are bounded ascending and limited to 512`() {
        reject(portfolioHistoryResponse(points = listOf(point(tao = "1"), point(tao = "2"))))
        reject(portfolioHistoryResponse(points = listOf(point(tao = "1", time = "2026-08-31T23:59:59Z"))))
        reject(portfolioHistoryResponse(points = listOf(point(tao = "1", time = "2026-10-01T00:00:01Z"))))
        reject(portfolioHistoryResponse(points = pointsWithinOneSecond(count = 513)))
        val endpoints = listOf(point(tao = "1", time = "2026-09-01T00:00:00Z"), point(tao = "2", time = "2026-10-01T00:00:00Z"))
        assertEquals(2, mapBittensorPortfolioHistory(portfolioHistoryResponse(points = endpoints), period).points.size)
        reject(portfolioHistoryResponse(points = endpoints.reversed()))
    }

    @Test
    fun `reported decimal budget rejects exponent whitespace malformed and missing fields`() {
        listOf(null, "", "1e2", "NaN", "+1", "01", ".1", "1.", " 1", "1".repeat(1025)).forEach { value ->
            reject(portfolioHistoryResponse(points = listOf(point(tao = value))))
            reject(portfolioHistoryResponse(points = listOf(point(tao = "1", usd = value))))
        }
        assertEquals(
            1024,
            mapBittensorPortfolioHistory(portfolioHistoryResponse(points = listOf(point(tao = "1".repeat(1024)))), period)
                .points.single().tao.toPlainString().length
        )
        reject(portfolioHistoryResponse(points = listOf(point(tao = "1", completed = null))))
    }

    @Test
    fun `fractional timestamps are validated before truncation and metadata retains precision`() {
        val first = point(tao = "0", time = "2026-09-01T00:00:00.000000001Z", completed = false)
        val last = point(tao = "-0.00", time = "2026-09-01T00:00:00.999999999Z")
        val response = portfolioHistoryResponse(
            points = listOf(first, last),
            start = "2026-09-01T00:00:00.000000001Z",
            end = "2026-09-01T00:00:00.999999999Z"
        )
        val result = mapBittensorPortfolioHistory(response, period)
        assertEquals(result.points.first().timestamp, result.points.last().timestamp)
        assertFalse(result.points.first().completed)
        assertTrue(result.status.asOf.toPlainString().endsWith(".999999999"))

        reject(portfolioHistoryResponse(points = listOf(last, first)))
        reject(portfolioHistoryResponse(points = listOf(first), start = "2026-09-01T00:00:00.000000002Z"))
        reject(portfolioHistoryResponse(points = listOf(last), end = "2026-09-01T00:00:00.999999998Z"))
    }

    @Test
    fun `exactly 512 distinct points are accepted`() {
        val points = pointsWithinOneSecond(count = 512)
        assertEquals(512, mapBittensorPortfolioHistory(portfolioHistoryResponse(points = points), period).points.size)
    }

    private fun pointsWithinOneSecond(count: Int) = List(count) { index ->
        point(tao = "1", time = "2026-09-02T00:00:00.${index.toString().padStart(9, '0')}Z")
    }

    private fun point(
        tao: String?,
        time: String = "2026-09-02T00:00:00Z",
        usd: String? = "400",
        completed: Boolean? = true
    ) = BittensorPortfolioHistoryRemote.Point(
        timestamp = time,
        reportedValueTao = tao,
        reportedValueUsd = usd,
        completed = completed
    )

    private fun reject(remote: BittensorPortfolioHistoryRemote) {
        assertTrue(runCatching { mapBittensorPortfolioHistory(remote, period) }.isFailure)
    }
}
