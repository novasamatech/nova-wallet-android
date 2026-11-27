package io.novafoundation.nova.common.view.input

import android.text.Spanned
import android.text.InputFilter
import android.widget.EditText
import io.novafoundation.nova.test_shared.any
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class DecimalInputFilterTest {

    private val amount = DecimalInputFilter(DecimalInputConstraints.Unlimited)

    private val fractionLimited = DecimalInputFilter(DecimalInputConstraints.FractionOnly(4))

    private fun boundedInput(fractionDigits: Int) = DecimalInputFilter(DecimalInputConstraints.Bounded(maxFractionDigits = fractionDigits, maxIntegerDigits = 15))

    @Test
    fun `typing and pasting either separator yields the same value in both modes`() {
        for (filter in listOf(amount, fractionLimited)) {
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
    fun `fraction limit applies to replacement edits`() {
        assertEquals("15.01234567", amount.edit("", "15,01234567"))
        assertEquals("0.0125", fractionLimited.edit("", "0,0125"))
        assertEquals("", fractionLimited.edit("", "0,01255"))
        assertEquals("0.0125", fractionLimited.edit("0.0125", "66", 3, 4))
        assertEquals("0.0625", fractionLimited.edit("0.0125", "6", 3, 4))
        assertEquals("0.", fractionLimited.edit("0.0125", "", 2, 6))
    }

    @Test
    fun `field accepts configured precision on typing and paste`() {
        for ((precision, accepted) in listOf(6 to "1.123456", 8 to "1.12345678", 12 to "1.123456789012")) {
            val filter = boundedInput(precision)
            assertEquals(accepted, filter.edit("", accepted.replace('.', ',')))
            assertEquals(accepted, filter.edit(accepted.dropLast(1), accepted.takeLast(1)))
            assertEquals(accepted, filter.edit(accepted, "9"))
            assertEquals("2", filter.edit("2", accepted + "9", 0, 1))
        }
    }

    @Test
    fun `bounded input limits integer digits independently of fractional digits`() {
        val filter = boundedInput(8)
        val maximum = "999999999999999.12345678"
        assertEquals(maximum, filter.edit("", maximum))
        assertEquals("", filter.edit("", "1000000000000000"))
        assertEquals("999999999999999", filter.edit("999999999999999", "9"))
        assertEquals("12", filter.edit("12", "1000000000000000", 0, 2))
        assertEquals("0.12345678", filter.edit("", "0.12345678"))
    }

    @Test
    fun `deleting separator cannot bypass the integer digit limit`() {
        val filter = boundedInput(8)
        val original = "999999999999999.1"

        assertEquals(original, filter.edit(original, "", 15, 16))
        assertEquals("99999999999999.1", filter.edit(original, "", 0, 1))
        assertEquals("", filter.edit(original, "", 0, original.length))
    }

    @Test
    fun `user can reduce excess digits after a constraint change without automatic rounding`() {
        val filter = boundedInput(6)
        val original = "1000000000000000.123456789"
        val shorter = original.dropLast(1)

        assertEquals(shorter, filter.edit(original, "", original.lastIndex, original.length))
        assertEquals(original, filter.edit(original, "8"))
        assertEquals("1.123456", filter.edit(original, "1.123456", 0, original.length))
        assertEquals("", filter.edit(original, "", 0, original.length))
    }

    @Test
    fun `leading zeros do not consume the integer limit`() {
        val filter = boundedInput(8)

        assertEquals("000999999999999999", filter.edit("", "000999999999999999"))
        assertEquals("", filter.edit("", "0001000000000000000"))
    }

    @Test
    fun `updating constraints replaces the old decimal filter and preserves other filters`() {
        val editText = mock(EditText::class.java)
        val otherFilter = mock(InputFilter::class.java)
        var currentFilters: Array<InputFilter> = arrayOf(otherFilter, amount)
        `when`(editText.filters).thenAnswer { currentFilters }
        doAnswer { currentFilters = it.getArgument(0); null }.`when`(editText).filters = any()

        editText.setDecimalInputConstraints(DecimalInputConstraints.Bounded(maxFractionDigits = 6, maxIntegerDigits = 15))
        editText.setDecimalInputConstraints(DecimalInputConstraints.Bounded(maxFractionDigits = 8, maxIntegerDigits = 15))

        assertEquals(2, currentFilters.size)
        assertSame(otherFilter, currentFilters.first())
        val activeFilter = currentFilters.filterIsInstance<DecimalInputFilter>().single()
        assertEquals("1.12345678", activeFilter.edit("", "1.12345678"))
        assertEquals("", activeFilter.edit("", "1.123456789"))

        editText.setDecimalInputConstraints(DecimalInputConstraints.Unlimited)
        val unrestricted = currentFilters.filterIsInstance<DecimalInputFilter>().single()
        assertEquals("1.123456789", unrestricted.edit("", "1.123456789"))
    }

    private fun DecimalInputFilter.edit(original: String, input: String, start: Int = original.length, end: Int = start): String {
        val destination = mock(Spanned::class.java)
        `when`(destination.toString()).thenReturn(original)
        `when`(destination.subSequence(start, end)).thenReturn(original.subSequence(start, end))

        val filtered = filter(input, 0, input.length, destination, start, end)
        return original.replaceRange(start, end, filtered ?: input)
    }
}
