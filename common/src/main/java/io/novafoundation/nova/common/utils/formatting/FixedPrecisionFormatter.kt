package io.novafoundation.nova.common.utils.formatting

import java.math.BigDecimal
import java.math.RoundingMode

class FixedPrecisionFormatter(private val precision: Int) : NumberFormatter {

    private val delegate = decimalFormatterFor(patternWith(precision))

    override fun format(number: BigDecimal, roundingMode: RoundingMode): String {
        if (delegate.roundingMode != roundingMode) {
            delegate.roundingMode = roundingMode
        }

        return delegate.format(number)
    }
}
