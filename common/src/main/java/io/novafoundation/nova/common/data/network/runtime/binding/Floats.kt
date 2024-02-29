package io.novafoundation.nova.common.data.network.runtime.binding

import java.math.BigDecimal
import java.math.BigInteger
import io.novafoundation.nova.common.utils.Perbill as PerbillTyped

typealias Perbill = BigDecimal
typealias FixedI64 = BigDecimal

const val PERBILL_MANTISSA_SIZE = 9
const val PERMILL_MANTISSA_SIZE = 6

@HelperBinding
fun bindPerbillNumber(value: BigInteger, mantissa: Int = PERBILL_MANTISSA_SIZE): Perbill {
    return value.toBigDecimal(scale = mantissa)
}

fun bindPerbill(dynamic: Any?, mantissa: Int = PERBILL_MANTISSA_SIZE): Perbill {
    return bindPerbillNumber(dynamic.cast(), mantissa)
}

fun bindFixedI64Number(value: BigInteger): FixedI64 {
    return bindPerbillNumber(value)
}

fun bindFixedI64(dynamic: Any?): FixedI64 {
    return bindPerbill(dynamic)
}

fun bindPerbillTyped(dynamic: Any?, mantissa: Int = PERBILL_MANTISSA_SIZE): PerbillTyped {
    return PerbillTyped(bindPerbill(dynamic, mantissa).toDouble())
}

fun bindPermill(dynamic: Any?): PerbillTyped {
    return bindPerbillTyped(dynamic, mantissa = PERMILL_MANTISSA_SIZE)
}
