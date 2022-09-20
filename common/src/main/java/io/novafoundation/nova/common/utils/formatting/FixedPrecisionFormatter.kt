package io.novafoundation.nova.common.utils.formatting

import java.math.BigDecimal

class FixedPrecisionFormatter(
    precision: Int
) : NumberFormatter {

    private val delegate = decimalFormatterFor(patternWith(precision))

    override fun format(number: BigDecimal): String {
        return delegate.format(number)
    }
}
