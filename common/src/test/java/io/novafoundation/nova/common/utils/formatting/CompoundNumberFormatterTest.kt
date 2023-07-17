package io.novafoundation.nova.common.utils.formatting

import org.junit.Test

class CompoundNumberFormatterTest {

    private val formatter = defaultNumberFormatter()

    @Test
    fun `should format all cases`() {
        testFormatter(formatter, "0.0000000116", "0.000000011676979")
        testFormatter(formatter, "0.0000216", "0.000021676979")
        testFormatter(formatter, "0.315", "0.315000041811")
        testFormatter(formatter, "0.99999", "0.99999999999")
        testFormatter(formatter, "999.99999", "999.99999999")
        testFormatter(formatter, "1M", "1000000")
        testFormatter(formatter, "888,888.12", "888888.1234")
        testFormatter(formatter, "1.24M", "1243000")
        testFormatter(formatter, "1.24M", "1243011")
        testFormatter(formatter, "100.04B", "100041000000")
        testFormatter(formatter, "1T", "1001000000000")
        testFormatter(formatter, "1,001T", "1001000000000000")
    }
}
