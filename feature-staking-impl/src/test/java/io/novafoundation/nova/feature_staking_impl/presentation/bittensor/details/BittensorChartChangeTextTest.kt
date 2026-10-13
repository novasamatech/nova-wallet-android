package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class BittensorChartChangeTextTest {

    @Test
    fun `formats the covered change with sign and one decimal`() {
        assertEquals("+12.5% · 30D", bittensorChartChangeText(100f, 112.5f, "30D", covered = true, Locale.US))
        assertEquals("-3.9% · 7D", bittensorChartChangeText(100f, 96.1f, "7D", covered = true, Locale.US))
        assertEquals("0% · 1D", bittensorChartChangeText(100f, 100f, "1D", covered = true, Locale.US))
    }

    @Test
    fun `hides the change when the history does not cover the period or the start is zero`() {
        assertEquals("— · 1D", bittensorChartChangeText(100f, 150f, "1D", covered = false, Locale.US))
        assertEquals("— · 3M", bittensorChartChangeText(0f, 150f, "3M", covered = true, Locale.US))
    }
}
