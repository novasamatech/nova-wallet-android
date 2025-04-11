package io.novafoundation.nova.common.utils

import android.os.Parcelable
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class Precision(val value: Int) : Parcelable

fun Int.asPrecision() = Precision(this)

fun BigDecimal.planksFromAmount(precision: Precision) = scaleByPowerOfTen(precision.value).toBigInteger()

fun BigInteger.amountFromPlanks(precision: Precision) = toBigDecimal(scale = precision.value)
