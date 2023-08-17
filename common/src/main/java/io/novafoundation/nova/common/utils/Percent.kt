@file:Suppress("NOTHING_TO_INLINE")

package io.novafoundation.nova.common.utils

import java.math.BigDecimal

/**
 * Type that represents [Percent] / 100
 * Thus, 0.1 will represent equivalent to 10%
 */
@JvmInline
value class Perbill(val value: Double) : Comparable<Perbill> {

    override fun compareTo(other: Perbill): Int {
        return value.compareTo(other.value)
    }
}

/**
 * Type that represents percentages
 * E.g. Percent(10) represents value of 10%
 */
@JvmInline
value class Percent(val value: Double) : Comparable<Percent> {

    override fun compareTo(other: Percent): Int {
        return value.compareTo(other.value)
    }
}

inline fun BigDecimal.asPerbill(): Perbill = Perbill(this.toDouble())

inline fun BigDecimal.asPercent(): Percent = Percent(this.toDouble())

inline fun Double.asPerbill(): Perbill = Perbill(this)

inline fun Double.asPercent(): Percent = Percent(this)

inline fun Percent.toPerbill(): Perbill = Perbill(value / 100)

inline fun Perbill.toPercent(): Percent = Percent(value * 100)
