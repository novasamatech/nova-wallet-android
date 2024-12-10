package io.novafoundation.nova.common.utils.formatting

import java.lang.Integer.max
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.min

class DynamicPrecisionFormatter(
    private val minScale: Int,
    private val minPrecision: Int,
) : NumberFormatter {

    private val patternCache = mutableMapOf<Int, DecimalFormat>()

    override fun format(number: BigDecimal, roundingMode: RoundingMode): String {
        // scale() - total amount of digits after 0.,
        // precision() - amount of non-zero digits in decimal part
        val zeroPrecision = number.scale() - number.precision()
        val requiredPrecision = zeroPrecision + min(number.precision(), minPrecision)

        val formattingPrecision = max(minScale, requiredPrecision)

        val formatter = patternCache.getOrPut(formattingPrecision) { decimalFormatterFor(patternWith(formattingPrecision)) }
        if (formatter.roundingMode != roundingMode) {
            formatter.roundingMode = roundingMode
        }

        return formatter.format(number)
    }
}
