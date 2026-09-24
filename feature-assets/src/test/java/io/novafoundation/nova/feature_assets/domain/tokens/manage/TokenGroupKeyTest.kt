package io.novafoundation.nova.feature_assets.domain.tokens.manage

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenGroupKeyTest {

    private val symbols = setOf("ETH", "ETH-Snowbridge", "SOL-Wormhole", "HOLLAR", "PINK-ISH")

    @Test
    fun `a plain token keeps its own row`() {
        assertEquals("ETH", tokenGroupKey("ETH", symbols))
        assertEquals("HOLLAR", tokenGroupKey("HOLLAR", symbols))
    }

    @Test
    fun `a bridged variant joins the token it stands for`() {
        assertEquals("ETH", tokenGroupKey("ETH-Snowbridge", symbols))
    }

    @Test
    fun `a variant with no parent in the list keeps its own row`() {
        // SOL itself is not among the assets, so folding SOL-Wormhole away would lose it
        assertEquals("SOL-Wormhole", tokenGroupKey("SOL-Wormhole", symbols))
        assertEquals("PINK-ISH", tokenGroupKey("PINK-ISH", symbols))
    }
}
