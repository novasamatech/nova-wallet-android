package io.novafoundation.nova.common.utils

import java.math.BigDecimal

/**
 * An exact decimal fraction: 0.003 means 0.3%. Allowed ranges belong to the consuming domain.
 * Preserves BigDecimal scale, including its scale-sensitive value equality.
 */
@JvmInline
value class DecimalFraction(val inFraction: BigDecimal) {

    val inPercents: BigDecimal
        get() = inFraction.movePointRight(2)
}
