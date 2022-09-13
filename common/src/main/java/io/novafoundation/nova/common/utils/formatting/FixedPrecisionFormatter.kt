package io.novafoundation.nova.common.utils.formatting

import java.math.BigDecimal
import java.math.RoundingMode

class FixedPrecisionFormatter(
    precision: Int,
    roundingMode: RoundingMode
) : NumberFormatter {

    private val delegate = decimalFormatterFor(patternWith(precision), roundingMode)

    override fun format(number: BigDecimal): String {
        return delegate.format(number)
    }
}
