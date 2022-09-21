package io.novafoundation.nova.common.utils

import java.math.BigDecimal
import java.math.RoundingMode

fun percentage(scale: Int, values: List<BigDecimal>): List<BigDecimal> {
    val total = values.sumOf { it }
    val percentage = values.map { it / total * BigDecimal.valueOf(100.0) }

    val accumulatedValues = List(percentage.size) { index ->
        val accumulated = percentage.subList(0, index + 1).sumOf { it }
        accumulated.setScale(scale, RoundingMode.HALF_UP)
    }

    val baseLine = accumulatedValues.mapIndexed { index, value ->
        if (index == 0) {
            BigDecimal.valueOf(0.0)
        } else {
            accumulatedValues[index - 1]
        }
    }

    return accumulatedValues.mapIndexed { index, value ->
        value - baseLine[index]
    }
}

fun percentage(scale: Int, vararg values: BigDecimal): List<BigDecimal> {
    return percentage(scale, values.toList())
}
