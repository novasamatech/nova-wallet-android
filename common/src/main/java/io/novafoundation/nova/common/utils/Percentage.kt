package io.novafoundation.nova.common.utils

import java.math.BigDecimal
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
