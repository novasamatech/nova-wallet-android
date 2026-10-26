package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Test

class NewChainFeaturesMappingTest {

    @Test
    fun `missing and malformed flags are empty`() {
        assertEquals(emptySet<Chain.NewFeature>(), mapNewChainFeatures(null))
        assertEquals(emptySet<Chain.NewFeature>(), mapNewChainFeatures("staking"))
    }

    @Test
    fun `known staking flag is recognized`() {
        assertEquals(setOf(Chain.NewFeature.STAKING), mapNewChainFeatures(listOf("staking")))
    }

    @Test
    fun `future flags and malformed entries are ignored`() {
        assertEquals(setOf(Chain.NewFeature.STAKING), mapNewChainFeatures(listOf("future", null, 1, "staking")))
    }

    @Test
    fun `duplicates collapse and matching is case sensitive`() {
        assertEquals(setOf(Chain.NewFeature.STAKING), mapNewChainFeatures(listOf("staking", "staking", "Staking")))
        assertEquals(emptySet<Chain.NewFeature>(), mapNewChainFeatures(listOf("Staking")))
    }
}
