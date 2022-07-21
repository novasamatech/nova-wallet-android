package io.novafoundation.nova.common.utils

import java.math.BigDecimal
import java.math.BigInteger


fun BigDecimal.planksFromAmount(precision: Int) = this.scaleByPowerOfTen(precision).toBigInteger()
fun BigInteger.amountFromPlanks(precision: Int) = toBigDecimal(scale = precision)
