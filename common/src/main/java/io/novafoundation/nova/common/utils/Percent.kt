@file:Suppress("NOTHING_TO_INLINE")

package io.novafoundation.nova.common.utils

import java.math.BigDecimal

/**
 * Type that represents [Percent] / 100
 * Thus, 0.1 will represent equivalent to 10%
 */
@JvmInline
@Deprecated("Use Fraction which offers much easier and understandable abstraction over fractions")
value class Perbill(val value: Double) : Comparable<Perbill> {

    companion object {

        fun zero() = Perbill(0.0)
    }

    override fun compareTo(other: Perbill): Int {
        return value.compareTo(other.value)
    }
}

/**
 * Type that represents percentages
 * E.g. Percent(10) represents value of 10%
 */
@JvmInline
@Deprecated("Use Fraction which offers much easier and understandable abstraction over fractions")
value class Percent(val value: Double) : Comparable<Percent> {

    companion object {

        fun zero(): Percent = Percent(0.0)
    }

    override fun compareTo(other: Percent): Int {
        return value.compareTo(other.value)
    }

    operator fun div(divisor: Int): Percent {
        return Percent(value / divisor)
    }
}


inline fun BigDecimal.asPerbill(): Perbill = Perbill(this.toDouble())

inline fun Double.asPerbill(): Perbill = Perbill(this)

inline fun Double.asPercent(): Percent = Percent(this)

inline fun Perbill.toPercent(): Percent = Percent(value * 100)

inline fun Perbill?.orZero(): Perbill = this ?: Perbill(0.0)
