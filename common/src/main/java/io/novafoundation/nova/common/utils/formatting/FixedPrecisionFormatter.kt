package io.novafoundation.nova.common.utils.formatting

import java.math.BigDecimal
import java.math.RoundingMode

class FixedPrecisionFormatter(private val precision: Int) : NumberFormatter {

    override fun format(number: BigDecimal, roundingMode: RoundingMode): String {
        val delegate = decimalFormatterFor(patternWith(precision), roundingMode)

        return delegate.format(number)
    }
}
