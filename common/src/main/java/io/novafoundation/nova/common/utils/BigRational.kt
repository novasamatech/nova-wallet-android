package io.novafoundation.nova.common.utils

import java.math.BigInteger

class BigRational(private val numerator: BigInteger, private val denominator: BigInteger) {

    val quotient: BigInteger
        get() = numerator / denominator

    companion object
}

fun BigRational.Companion.fixedU128(value: BigInteger): BigInteger {
    return BigRational(value, BigInteger.TEN.pow(18)).quotient
}
