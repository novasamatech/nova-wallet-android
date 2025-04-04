package io.novafoundation.nova.common.utils

import java.math.BigDecimal
import java.math.BigInteger

class BigRational(numerator: BigInteger, denominator: BigInteger) {

    val quotient: BigDecimal = numerator.toBigDecimal().divide(denominator.toBigDecimal())

    val integralQuotient: BigInteger = quotient.toBigInteger()

    companion object
}

fun BigRational.Companion.fixedU128(value: BigInteger): BigRational {
    return BigRational(value, BigInteger.TEN.pow(18))
}
