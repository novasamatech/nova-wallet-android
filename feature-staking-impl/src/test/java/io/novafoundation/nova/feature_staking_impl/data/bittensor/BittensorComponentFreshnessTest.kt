package io.novafoundation.nova.feature_staking_impl.data.bittensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorComponentFreshnessTest {

    @Test
    fun `available fresh and stale components retain distinct freshness`() {
        val fresh = BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = "FRESH")
        val stale = BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = "STALE")

        assertTrue(requireAvailableBittensorComponent(fresh, name = "Prices"))
        assertFalse(requireAvailableBittensorComponent(stale, name = "Prices"))
    }

    @Test
    fun `missing unavailable and unknown component states fail instead of becoming stale`() {
        val rejected = listOf(
            null,
            BittensorRecommendationsRemote.Component(availability = "UNAVAILABLE", freshness = "FRESH"),
            BittensorRecommendationsRemote.Component(availability = "UNKNOWN", freshness = "FRESH"),
            BittensorRecommendationsRemote.Component(availability = null, freshness = "FRESH"),
            BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = "UNKNOWN"),
            BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = null)
        )

        for (component in rejected) {
            val failure = runCatching { requireAvailableBittensorComponent(component, name = "Prices") }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
        }
    }

    @Test
    fun `only available fresh recommendations are fresh`() {
        val fresh = BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = "FRESH")

        assertTrue(hasFreshBittensorRecommendations(recommendationMeta(fresh)))

        val notFresh = listOf(
            BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = "STALE"),
            BittensorRecommendationsRemote.Component(availability = "UNAVAILABLE", freshness = "FRESH"),
            BittensorRecommendationsRemote.Component(availability = "UNKNOWN", freshness = "FRESH"),
            BittensorRecommendationsRemote.Component(availability = null, freshness = "FRESH"),
            BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = "UNKNOWN"),
            BittensorRecommendationsRemote.Component(availability = "AVAILABLE", freshness = null)
        )

        for (component in notFresh) {
            assertFalse(hasFreshBittensorRecommendations(recommendationMeta(component)))
        }
    }

    @Test
    fun `missing recommendation metadata is not fresh`() {
        assertFalse(hasFreshBittensorRecommendations(meta = null))
        assertFalse(hasFreshBittensorRecommendations(BittensorRecommendationsRemote.Meta(clientGates = null, components = null)))
        assertFalse(hasFreshBittensorRecommendations(recommendationMeta(component = null)))
    }

    private fun recommendationMeta(component: BittensorRecommendationsRemote.Component?): BittensorRecommendationsRemote.Meta {
        val components = BittensorRecommendationsRemote.Components(recommendations = component)
        return BittensorRecommendationsRemote.Meta(clientGates = null, components)
    }
}
