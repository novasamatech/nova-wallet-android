package io.novafoundation.nova.common.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinExtTest {

    @Test
    fun `integerDigits counts integer part regardless of sign scale or exponent`() {
        val cases = mapOf(
            "123.45" to 3,
            "123.4500" to 3,
            "-123.45" to 3,
            "1000" to 4,
            "1E+3" to 4,
            "0.001" to 1,
            "-0.001" to 1,
            "0" to 1,
            "0.000" to 1,
            "0E+3" to 1
        )

        for ((value, expected) in cases) {
            val amount = value.toBigDecimal()
            assertEquals(value, expected, amount.integerDigits)
            assertEquals("stripped $value", expected, amount.stripTrailingZeros().integerDigits)
        }
    }

    @Test
    fun `endsWith should return false for suffix bigger than content`() {
        assertFalse(byteArrayOf(0, 1).endsWith(byteArrayOf(0, 1, 2)))
    }

    @Test
    fun `endsWith should return true for suffix equal to the content`() {
        assertTrue(byteArrayOf(0, 1, 2).endsWith(byteArrayOf(0, 1, 2)))
    }

    @Test
    fun `endsWith should return true for correct suffix`() {
        assertTrue(byteArrayOf(0, 1, 2).endsWith(byteArrayOf(1, 2)))
    }

    @Test
    fun `endsWith should return true for incorrect suffix`() {
        assertFalse(byteArrayOf(0, 1, 2, 3).endsWith(byteArrayOf(1, 2)))
        assertFalse(byteArrayOf(0, 1, 2, 3).endsWith(byteArrayOf(2)))
    }
}
