package io.novafoundation.nova.common.utils.formatting

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class CompoundNumberFormatter(
    val abbreviations: List<NumberAbbreviation>,
) : NumberFormatter {

    init {
        require(abbreviations.isNotEmpty()) {
            "Cannot create compound formatter with empty abbreviations"
        }

        require(
            abbreviations.zipWithNext().all { (current, next) ->
                current.threshold <= next.threshold
            }
        ) {
            "Abbreviations should go in non-descending order w.r.t. threshold"
        }
    }

    override fun format(number: BigDecimal, roundingMode: RoundingMode): String {
        val lastAbbreviationMatching = abbreviations.lastOrNull { number >= it.threshold } ?: abbreviations.first()

        val scaled = number.divide(lastAbbreviationMatching.divisor, MathContext.UNLIMITED)

        return lastAbbreviationMatching.formatter.format(scaled, roundingMode) + lastAbbreviationMatching.suffix
    }
}
