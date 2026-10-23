package io.novafoundation.nova.feature_swap_impl.presentation.options

import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator.Method
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlippageSettingsSheetTest {

    private val config = SlippageSettingsPayload(
        selected = 0.005,
        default = 0.005,
        tips = listOf(0.001, 0.005, 0.01, 0.03),
        minimum = 0.0001,
        maximum = 0.5,
        small = 0.001,
        big = 0.03
    ).config()

    @Test
    fun `custom input accepts at most four decimals like iOS`() {
        assertTrue(slippageInputWithinPrecision("", SLIPPAGE_MAX_FRACTION_DIGITS))
        assertTrue(slippageInputWithinPrecision("12", SLIPPAGE_MAX_FRACTION_DIGITS))
        assertTrue(slippageInputWithinPrecision("0.", SLIPPAGE_MAX_FRACTION_DIGITS))
        assertTrue(slippageInputWithinPrecision("0.0125", SLIPPAGE_MAX_FRACTION_DIGITS))
        assertTrue(slippageInputWithinPrecision("0,0125", SLIPPAGE_MAX_FRACTION_DIGITS))
        assertFalse(slippageInputWithinPrecision("0.01255", SLIPPAGE_MAX_FRACTION_DIGITS))
        assertFalse(slippageInputWithinPrecision("1,00001", SLIPPAGE_MAX_FRACTION_DIGITS))
    }

    @Test
    fun `typed preset values are reported as presets despite floating point paths`() {
        assertEquals(Method.PRESET, slippageChangeMethod(0.005.fractions, 0.1.percents, config))
        assertEquals(Method.PRESET, slippageChangeMethod(0.005.fractions, 3.0.percents, config))
    }

    @Test
    fun `returning to the default is a reset only when the default was not already selected`() {
        assertEquals(Method.RESET, slippageChangeMethod(0.01.fractions, 0.5.percents, config))
        assertEquals(Method.PRESET, slippageChangeMethod(0.005.fractions, 0.5.percents, config))
    }

    @Test
    fun `other values are custom`() {
        assertEquals(Method.CUSTOM, slippageChangeMethod(0.005.fractions, 0.0125.percents, config))
    }
}
