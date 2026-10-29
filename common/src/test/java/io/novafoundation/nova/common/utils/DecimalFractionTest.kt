package io.novafoundation.nova.common.utils

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DecimalFractionTest {

    @Test
    fun `fraction preserves the exact decimal value and scale`() {
        val value = BigDecimal("0.1234567890123456789012345678900")
        val fraction = DecimalFraction(value)

        assertSame(value, fraction.inFraction)
        assertEquals(BigDecimal("12.34567890123456789012345678900"), fraction.inPercents)
        assertEquals(DecimalFraction(BigDecimal("0.1234567890123456789012345678900")), fraction)
        assertNotEquals(DecimalFraction(value.stripTrailingZeros()), fraction)
    }

    @Test
    fun `decimal fraction is not a whole percentage`() {
        val fraction = DecimalFraction(BigDecimal("0.003"))

        assertEquals(BigDecimal("0.003"), fraction.inFraction)
        assertEquals(BigDecimal("0.3"), fraction.inPercents)
    }

    @Test
    fun `range restrictions are left to the consuming domain`() {
        for (value in listOf("0", "-0.1", "1", "2")) {
            val fraction = DecimalFraction(BigDecimal(value))

            assertEquals(BigDecimal(value), fraction.inFraction)
            assertEquals(BigDecimal(value).movePointRight(2), fraction.inPercents)
        }
    }

    @Test
    fun `small fractions do not underflow through floating point`() {
        val fraction = DecimalFraction(BigDecimal("1e-400"))

        assertEquals(BigDecimal("1e-400"), fraction.inFraction)
        assertEquals(BigDecimal("1e-398"), fraction.inPercents)
    }
}
