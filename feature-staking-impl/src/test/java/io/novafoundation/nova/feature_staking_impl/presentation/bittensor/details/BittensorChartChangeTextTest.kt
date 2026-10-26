package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorChartChangeTextTest {

    @Test
    fun `missing return keeps the selected portfolio period`() {
        assertEquals("— · 30D", formatBittensorChartChangeText(null, "30D", missingText = "— · 30D"))
        assertEquals("— · 3M", formatBittensorChartChangeText(null, "3M", missingText = "— · 3M"))
    }

    @Test
    fun `unrelated charts retain their existing missing return policy`() {
        assertEquals("", formatBittensorChartChangeText(null, "7D"))
    }
}
