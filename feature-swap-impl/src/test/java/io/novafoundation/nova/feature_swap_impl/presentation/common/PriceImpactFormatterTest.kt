package io.novafoundation.nova.feature_swap_impl.presentation.common

import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceImpactFormatterTest {

    private val thresholds = PriceImpactThresholds(
        lowPriceImpact = 1.percents,
        mediumPriceImpact = 5.percents,
        highPriceImpact = 15.percents
    )

    @Test
    fun `price difference appears from one percent`() {
        assertNull(thresholds.colorFor(0.99.percents))
        assertEquals(R.color.text_secondary, thresholds.colorFor(1.percents))
    }

    @Test
    fun `warning and high impact keep their original cutoffs`() {
        assertEquals(R.color.text_secondary, thresholds.colorFor(5.percents))
        assertEquals(R.color.text_warning, thresholds.colorFor(6.percents))
        assertEquals(R.color.text_warning, thresholds.colorFor(15.percents))
        assertEquals(R.color.text_negative, thresholds.colorFor(16.percents))
    }
}
