package io.novafoundation.nova.common.utils

import java.math.BigDecimal

@JvmInline
value class Fraction private constructor(private val value: Double) : Comparable<Fraction> {

    companion object {

        val ZERO: Fraction = Fraction(0.0)

        fun Double.toFraction(unit: FractionUnit): Fraction {
            return Fraction(unit.convertToFraction(this))
        }

        val Double.percents: Fraction
            get() = toFraction(FractionUnit.PERCENT)

        val BigDecimal.percents: Fraction
            get() = toDouble().percents

        val BigDecimal.fractions: Fraction
            get() = toDouble().fractions

        val Double.fractions: Fraction
            get() = toFraction(FractionUnit.FRACTION)

        val Int.percents: Fraction
            get() = toDouble().toFraction(FractionUnit.PERCENT)
    }

    val inPercents: Double
        get() = FractionUnit.PERCENT.convertFromFraction(value)

    val inFraction: Double
        get() = FractionUnit.FRACTION.convertFromFraction(value)

    val inWholePercents: Int
        get() = FractionUnit.PERCENT.convertFromFractionWhole(value)

    override fun compareTo(other: Fraction): Int {
        return value.compareTo(other.value)
    }
}

enum class FractionUnit {

    /**
     * Default range: 0..1
     */
    FRACTION,

    /**
     * Default range: 0..100
     */
    PERCENT
}

fun Fraction?.orZero(): Fraction = this ?: Fraction.ZERO

private fun FractionUnit.convertToFraction(value: Double): Double {
    return when (this) {
        FractionUnit.FRACTION -> value
        FractionUnit.PERCENT -> value / 100
    }
}

private fun FractionUnit.convertFromFraction(value: Double): Double {
    return when (this) {
        FractionUnit.FRACTION -> value
        FractionUnit.PERCENT -> value * 100
    }
}

private fun FractionUnit.convertFromFractionWhole(value: Double): Int {
    return convertFromFraction(value).toInt()
}
