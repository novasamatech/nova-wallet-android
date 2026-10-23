package io.novafoundation.nova.common.utils.formatting

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CompactNumberFormatterTest {

    private fun compact(value: String) = BigDecimal(value).formatCompact()

    @Test
    fun `values below ten keep the regular precision`() {
        assertEquals("0", compact("0"))
        assertEquals("0.00657", compact("0.006571125"))
        assertEquals("1.23456", compact("1.234567"))
        assertEquals("9.99999", compact("9.999999"))
    }

    @Test
    fun `values from ten to a thousand keep two decimals`() {
        assertEquals("10", compact("10"))
        assertEquals("123.45", compact("123.4567"))
        assertEquals("999.99", compact("999.999"))
    }

    @Test
    fun `thousands use K with one decimal rounded down`() {
        assertEquals("1K", compact("1000"))
        assertEquals("8.4K", compact("8412.7"))
        assertEquals("54.2K", compact("54299.99"))
        assertEquals("67K", compact("67000"))
        assertEquals("210K", compact("210040"))
        assertEquals("999.9K", compact("999950"))
        assertEquals("999.9K", compact("999999.99"))
    }

    @Test
    fun `millions and above use two decimals`() {
        assertEquals("1M", compact("1000000"))
        assertEquals("1.02M", compact("1029999"))
        assertEquals("71.8M", compact("71800000"))
        assertEquals("2.19B", compact("2199999999"))
        assertEquals("1T", compact("1000000000000"))
    }

    @Test
    fun `negative values mirror positive ones`() {
        assertEquals("-8.4K", compact("-8412.7"))
        assertEquals("-1.02M", compact("-1029999"))
        assertEquals("-123.45", compact("-123.4567"))
    }

    @Test
    fun `rounding mode is respected`() {
        assertEquals("8.5K", BigDecimal("8450").formatCompact(RoundingMode.HALF_UP))
    }
}
