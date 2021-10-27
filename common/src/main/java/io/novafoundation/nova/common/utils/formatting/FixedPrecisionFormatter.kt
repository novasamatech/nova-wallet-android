package io.novafoundation.nova.common.utils.formatting

import io.novafoundation.nova.common.utils.decimalFormatterFor
import io.novafoundation.nova.common.utils.patternWith
import java.math.BigDecimal

class FixedPrecisionFormatter(
    private val precision: Int
) : NumberFormatter {

    private val delegate = decimalFormatterFor(patternWith(precision))

    override fun format(number: BigDecimal): String {
        return delegate.format(number)
    }
}
