package io.novafoundation.nova.feature_currency_api.presentation.formatters

import io.novafoundation.nova.common.utils.formatting.currencyFormatter
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import java.math.BigDecimal

private val currencyFormatter = currencyFormatter()

fun BigDecimal.formatAsCurrency(currency: Currency): String {
    return formatAsCurrency(currency.symbol, currency.code)
}

fun BigDecimal.formatAsCurrency(symbol: String?, code: String): String {
    val currencySymbol = symbol ?: "$code "
    return currencySymbol + currencyFormatter.format(this)
}
