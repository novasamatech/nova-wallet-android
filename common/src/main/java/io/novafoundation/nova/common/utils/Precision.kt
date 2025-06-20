package io.novafoundation.nova.common.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger

@JvmInline
@Parcelize
value class Precision(val value: Int) : Parcelable

fun Int.asPrecision() = Precision(this)

fun BigDecimal.planksFromAmount(precision: Precision) = planksFromAmount(precision.value)

fun BigDecimal.planksFromAmount(precision: Int) = scaleByPowerOfTen(precision).toBigInteger()

fun BigInteger.amountFromPlanks(precision: Precision) = toBigDecimal(scale = precision.value)
