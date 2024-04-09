package io.novafoundation.nova.common.utils

import java.math.BigDecimal
import java.math.BigInteger

@JvmInline
value class Precision(val value: Int)

fun Int.asPrecision() = Precision(this)

fun BigDecimal.planksFromAmount(precision: Precision) = scaleByPowerOfTen(precision.value).toBigInteger()

fun BigInteger.amountFromPlanks(precision: Precision) = toBigDecimal(scale = precision.value)
