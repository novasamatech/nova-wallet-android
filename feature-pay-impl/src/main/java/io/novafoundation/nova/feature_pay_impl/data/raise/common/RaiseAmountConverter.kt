package io.novafoundation.nova.feature_pay_impl.data.raise.common

import java.math.BigDecimal

interface RaiseAmountConverter {

    fun convertToApiAmount(domainAmount: BigDecimal, precision: Int): Long

    fun convertFromApiAmount(apiAmount: Long, precision: Int): BigDecimal
}

fun RaiseAmountConverter.convertToApiCurrency(domainAmount: BigDecimal): Long {
    return convertToApiAmount(domainAmount, precision = 2)
}

fun RaiseAmountConverter.convertFromApiCurrency(apiAmount: Long): BigDecimal {
    return convertFromApiAmount(apiAmount, precision = 2)
}

class RealRaiseAmountConverter() : RaiseAmountConverter {

    override fun convertToApiAmount(domainAmount: BigDecimal, precision: Int): Long {
        return domainAmount.scaleByPowerOfTen(precision).toLong()
    }

    override fun convertFromApiAmount(apiAmount: Long, precision: Int): BigDecimal {
        return apiAmount.toBigDecimal().scaleByPowerOfTen(-precision)
    }
}
