package io.novafoundation.nova.common.data.network.runtime.binding

import java.math.BigDecimal
import java.math.BigInteger
import io.novafoundation.nova.common.utils.Perbill as PerbillTyped

typealias Perbill = BigDecimal
typealias FixedI64 = BigDecimal

private const val FLOAT_MANTISSA_SIZE = 9

@HelperBinding
fun bindPerbillNumber(value: BigInteger): Perbill {
    return value.toBigDecimal(scale = FLOAT_MANTISSA_SIZE)
}

fun bindPerbill(dynamic: Any?): Perbill {
    return bindPerbillNumber(dynamic.cast())
}

fun bindFixedI64Number(value: BigInteger): FixedI64 {
    return bindPerbillNumber(value)
}

fun bindFixedI64(dynamic: Any?): FixedI64 {
    return bindPerbill(dynamic)
}

fun bindPerbillTyped(dynamic: Any?): PerbillTyped {
    return PerbillTyped(bindPerbill(dynamic).toDouble())
}
