package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val CHAIN_ID = "68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f"

private val CURATED = FullChainAssetId(CHAIN_ID, 0)
private val OTHER = FullChainAssetId(CHAIN_ID, 7)

class AssetVisibilityTest {

    private fun visibility(
        defaults: Set<FullChainAssetId> = setOf(CURATED),
        choices: Map<FullChainAssetId, Boolean> = emptyMap()
    ) = AssetVisibility(defaults, choices)

    @Test
    fun `a curated token is shown with no row of its own`() {
        assertTrue(visibility().isVisible(CURATED))
    }

    @Test
    fun `a token nobody has decided about stays out of the way`() {
        assertFalse(visibility().isVisible(OTHER))
    }

    @Test
    fun `a stored show wins over the token being uncurated`() {
        // This is the row balance discovery and a self-transfer both write
        assertTrue(visibility(choices = mapOf(OTHER to true)).isVisible(OTHER))
    }

    @Test
    fun `a stored hide wins over the curated list`() {
        assertFalse(visibility(choices = mapOf(CURATED to false)).isVisible(CURATED))
    }

    @Test
    fun `a config that could not be read hides nothing`() {
        // An empty list means "no opinion", never "hide everything"
        assertTrue(visibility(defaults = emptySet()).isVisible(OTHER))
    }

    @Test
    fun `a stored hide survives an unreadable config`() {
        assertFalse(visibility(defaults = emptySet(), choices = mapOf(OTHER to false)).isVisible(OTHER))
    }
}
