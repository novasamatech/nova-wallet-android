package io.novafoundation.nova.common.utils.formatting

import java.lang.Integer.max
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

class DynamicPrecisionFormatter(
    private val minScale: Int,
    private val minPrecision: Int,
) : NumberFormatter {

    override fun format(number: BigDecimal, roundingMode: RoundingMode): String {
        // scale() - total amount of digits after 0.,
        // precision() - amount of non-zero digits in decimal part
        val zeroPrecision = number.scale() - number.precision()
        val requiredPrecision = zeroPrecision + min(number.precision(), minPrecision)

        val formattingPrecision = max(minScale, requiredPrecision)

        return decimalFormatterFor(patternWith(formattingPrecision), roundingMode).format(number)
    }
}
