package io.novafoundation.nova.common.utils.formatting

import java.math.BigDecimal
import java.math.RoundingMode

interface NumberFormatter {

    fun format(number: BigDecimal, roundingMode: RoundingMode = RoundingMode.FLOOR): String
}
