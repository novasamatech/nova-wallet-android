package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.feature_staking_impl.R
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorYieldFormattingTest {

    @Test
    fun `unknown positive and negative rates have distinct colors`() {
        assertEquals(R.color.text_secondary, bittensorYieldColor(null))
        assertEquals(R.color.text_positive, bittensorYieldColor(BigDecimal("32.4")))
        assertEquals(R.color.text_negative, bittensorYieldColor(BigDecimal("-1.2")))
    }

    @Test
    fun `long fractional rates use the shared compact percentage formatter`() {
        val rate = BigDecimal("32.40303493327508")
        val text = rate.formatAsPercentage()
        assertTrue(text.length < 10)
        assertTrue(text.endsWith("%"))
        assertTrue(rate.negate().formatAsPercentage().startsWith("-"))
    }
}
