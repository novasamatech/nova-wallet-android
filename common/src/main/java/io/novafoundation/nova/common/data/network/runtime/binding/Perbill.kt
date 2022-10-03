package io.novafoundation.nova.common.data.network.runtime.binding

import java.math.BigDecimal
import java.math.BigInteger

typealias Perbill = BigDecimal

private const val PERBILL_MANTISSA_SIZE = 9

@HelperBinding
fun bindPerbill(value: BigInteger): Perbill {
    return value.toBigDecimal(scale = PERBILL_MANTISSA_SIZE)
}

fun bindPerbill(dynamic: Any?): Perbill {
    return bindPerbill(dynamic.cast())
}
