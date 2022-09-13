package io.novafoundation.nova.common.utils.formatting

import java.math.RoundingMode
import org.junit.Test

class FixedPrecisionFormatterTest {

    private val formatter = FixedPrecisionFormatter(2, RoundingMode.FLOOR)

    @Test
    fun `test format`() {
        testFormatter(formatter, "1.23", "1.2345")
        testFormatter(formatter, "1.2", "1.2")
        testFormatter(formatter, "1.23", "1.23")
        testFormatter(formatter, "1", "1")
        testFormatter(formatter, "123,456", "123456")
    }
}
