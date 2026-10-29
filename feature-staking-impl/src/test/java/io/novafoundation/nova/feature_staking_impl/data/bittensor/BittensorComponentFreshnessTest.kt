package io.novafoundation.nova.feature_staking_impl.data.bittensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorComponentFreshnessTest {

    @Test
    fun `available fresh and stale components retain distinct freshness`() {
        val fresh = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
        val stale = BittensorRecommendationsRemote.Component("AVAILABLE", "STALE")

        assertTrue(requireAvailableBittensorComponent(fresh, "Prices"))
        assertFalse(requireAvailableBittensorComponent(stale, "Prices"))
    }

    @Test
    fun `missing unavailable and unknown component states fail instead of becoming stale`() {
        val rejected = listOf(
            null,
            BittensorRecommendationsRemote.Component("UNAVAILABLE", "FRESH"),
            BittensorRecommendationsRemote.Component("UNKNOWN", "FRESH"),
            BittensorRecommendationsRemote.Component("AVAILABLE", "UNKNOWN")
        )

        for (component in rejected) {
            assertTrue(runCatching { requireAvailableBittensorComponent(component, "Prices") }.exceptionOrNull() is IllegalArgumentException)
        }
    }
}
