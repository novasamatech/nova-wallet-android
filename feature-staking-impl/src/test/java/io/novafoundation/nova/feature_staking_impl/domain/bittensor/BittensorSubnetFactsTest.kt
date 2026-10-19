package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRankingRemote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class BittensorSubnetFactsTest {

    @Test
    fun `same qualifying thresholds as iOS`() {
        val result = mapBittensorSubnetFacts(ranking(age = 640.days, pool = "25000.01", score = "49.99"), 12.seconds)
        assertEquals(21, result.monthsOld)
        assertTrue(result.deepPool)
        assertTrue(result.steady)
    }

    @Test
    fun `boundary values do not qualify`() {
        val result = mapBittensorSubnetFacts(ranking(age = 180.days, pool = "25000", score = "50"), 12.seconds)
        assertEquals(BittensorSubnetFacts(null, false, false), result)
    }

    @Test
    fun `age uses actual block time and rounds months down`() {
        val row = ranking(age = 181.days)
        assertEquals(6, mapBittensorSubnetFacts(row, 12.seconds).monthsOld)
        assertEquals(null, mapBittensorSubnetFacts(row, 6.seconds).monthsOld)
    }

    @Test
    fun `missing ranking does not fabricate facts`() {
        assertEquals(BittensorSubnetFacts(null, false, false), mapBittensorSubnetFacts(null, 12.seconds))
    }

    @Test
    fun `missing raw volatility must not produce steady chip`() {
        assertFalse(mapBittensorSubnetFacts(ranking(score = "0", raw = null), 12.seconds).steady)
    }

    @Test
    fun `invalid optional metrics do not produce chips`() {
        val result = mapBittensorSubnetFacts(ranking(pool = "NaN", score = "-1", raw = "invalid"), Duration.ZERO)
        assertEquals(BittensorSubnetFacts(null, false, false), result)
    }

    @Test
    fun `missing metrics are not interpreted as zero`() {
        val result = mapBittensorSubnetFacts(ranking(pool = null, score = null, raw = null), 12.seconds)
        assertEquals(BittensorSubnetFacts(null, false, false), result)
    }

    private fun ranking(
        age: Duration = Duration.ZERO,
        pool: String? = null,
        score: String? = null,
        raw: String? = "0.1"
    ): BittensorSubnetRankingRemote {
        val volatility = BittensorSubnetRankingRemote.MetricScore(raw, score)
        val breakdown = BittensorSubnetRankingRemote.Breakdown(volatility)
        return BittensorSubnetRankingRemote(1, pool, (age.inWholeSeconds / 12).toBigInteger(), breakdown)
    }
}
