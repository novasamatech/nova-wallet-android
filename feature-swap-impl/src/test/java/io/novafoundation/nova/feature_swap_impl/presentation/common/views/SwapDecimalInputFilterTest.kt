package io.novafoundation.nova.feature_swap_impl.presentation.common.views

import android.text.Spanned
import io.novafoundation.nova.feature_swap_impl.presentation.options.SLIPPAGE_MAX_FRACTION_DIGITS
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SwapDecimalInputFilterTest {

    private val amount = SwapDecimalInputFilter(maxFractionDigits = null)

    private val slippage = SwapDecimalInputFilter(maxFractionDigits = SLIPPAGE_MAX_FRACTION_DIGITS)

    @Test
    fun `typing and pasting either separator yields the same decimal amount and slippage`() {
        for (filter in listOf(amount, slippage)) {
            for (separator in listOf(".", ",")) {
                val integer = filter.edit("", "1")
                val fraction = filter.edit(integer, separator)
                assertEquals("1.5", filter.edit(fraction, "5"))
                assertEquals("1.5", filter.edit("", "1${separator}5"))
            }
        }
    }

    @Test
    fun `malformed pastes preserve the full original selection`() {
        for (pasted in listOf("1.2,3", "1,2,3", "1 234", "-15", "1e5", "1\u066B5", "\u0661,\u0665", "\u06F1,\u06F5")) {
            assertEquals("20", amount.edit("20", pasted, 0, 2))
        }
        assertEquals("1.5", amount.edit("1.5", ","))
        assertEquals("1.5", amount.edit("1.5", ",", 1, 2))
        assertEquals("15", amount.edit("1.5", "", 1, 2))
    }

    @Test
    fun `only slippage limits fractional digits including replacement edits`() {
        assertEquals("15.01234567", amount.edit("", "15,01234567"))
        assertEquals("0.0125", slippage.edit("", "0,0125"))
        assertEquals("", slippage.edit("", "0,01255"))
        assertEquals("0.0125", slippage.edit("0.0125", "66", 3, 4))
        assertEquals("0.0625", slippage.edit("0.0125", "6", 3, 4))
        assertEquals("0.", slippage.edit("0.0125", "", 2, 6))
    }

    private fun SwapDecimalInputFilter.edit(original: String, input: String, start: Int = original.length, end: Int = start): String {
        val destination = mock(Spanned::class.java)
        `when`(destination.toString()).thenReturn(original)
        `when`(destination.subSequence(start, end)).thenReturn(original.subSequence(start, end))

        val filtered = filter(input, 0, input.length, destination, start, end)
        return original.replaceRange(start, end, filtered ?: input)
    }
}
