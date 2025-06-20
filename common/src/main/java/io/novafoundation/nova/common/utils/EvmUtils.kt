package io.novafoundation.nova.common.utils

import java.math.BigDecimal
import java.math.BigInteger

typealias Gwei = BigDecimal

fun Gwei.gWeiToPlanks(): BigInteger = planksFromAmount(precision = 9)
