package io.novafoundation.nova.feature_currency_api.presentation.formatters

import io.novafoundation.nova.common.utils.formatting.currencyFormatter
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode

private val currencyFormatter = currencyFormatter()

fun BigDecimal.formatAsCurrency(currency: Currency, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return formatAsCurrency(currency.symbol, currency.code, roundingMode)
}

fun BigDecimal.formatAsCurrency(symbol: String?, code: String, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    val currencySymbol = symbol ?: "$code "
    return currencySymbol + currencyFormatter.format(this, roundingMode)
}
