package io.novafoundation.nova.common.utils

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

fun percentage(scale: Int, values: List<BigDecimal>): List<BigDecimal> {
    val total = values.sumOf { it }
    if (total.isZero) {
        return values.map { BigDecimal.ZERO }
    }

    val accumulatedPercentage = values.map { it / total * BigDecimal.valueOf(100.0) }
        .runningReduce { accumulated, next -> accumulated + next }
        .map { it.setScale(scale, RoundingMode.HALF_UP) }

    val baseLine = accumulatedPercentage.mapIndexed { index, value ->
        if (index == 0) {
            BigDecimal.ZERO
        } else {
            accumulatedPercentage[index - 1]
        }
    }

    return accumulatedPercentage.mapIndexed { index, value ->
        value - baseLine[index]
    }
}

fun percentage(scale: Int, vararg values: BigDecimal): List<BigDecimal> {
    return percentage(scale, values.toList())
}

/**
 * Splits this BigInteger "total" into parts according to the supplied [weights].
 *
 * Returns:
 *  - An empty list if [weights] is empty.
 *  - All zeroes if [this] (the total) is negative or any of [weights] are negative.
 *  - All zeroes if the sum of [weights] is zero.
 *  - Otherwise, parts distributed as proportionally as possible, with leftover
 *    integer units allocated to the parts with the largest fractional remainders.
 */
fun BigInteger.splitByWeights(weights: List<BigInteger>): List<BigInteger> {
    if (weights.isEmpty()) {
        return emptyList()
    }

    if (this < BigInteger.ZERO || weights.any { it < BigInteger.ZERO }) {
        return List(weights.size) { BigInteger.ZERO }
    }

    val sumOfWeights = weights.sum()
    if (sumOfWeights == BigInteger.ZERO) {
        return List(weights.size) { BigInteger.ZERO }
    }

    val weightedTotals = weights.map { w -> w * this }
    val baseParts = weightedTotals.mapTo(mutableListOf()) { wt -> wt.divide(sumOfWeights) }

    val remainders = weightedTotals.map { wt -> wt.mod(sumOfWeights) }

    val sumOfBaseParts = baseParts.sum()
    var leftover = this - sumOfBaseParts

    // Distribute leftover among those with largest remainder first
    val indicesByRemainder = remainders.indices.sortedByDescending { remainders[it] }
    for (i in indicesByRemainder) {
        if (leftover <= BigInteger.ZERO) break
        baseParts[i] = baseParts[i] + BigInteger.ONE
        leftover -= BigInteger.ONE
    }

    return baseParts
}
