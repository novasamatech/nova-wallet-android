package io.novafoundation.nova.common.utils.formatting

import org.junit.Test

class DynamicPrecisionFormatterTest {
    private val formatter = DynamicPrecisionFormatter(2)

    @Test
    fun `test format`() {
        testFormatter(formatter, "0.123", "0.123")
        testFormatter(formatter, "0.00001", "0.00001")
        testFormatter(formatter, "0.0000123", "0.0000123")
        testFormatter(formatter, "0.0000123", "0.000012345")
    }
}
