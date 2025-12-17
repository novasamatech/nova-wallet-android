package io.novafoundation.nova.feature_currency_api.presentation.formatters

import io.novafoundation.nova.common.utils.formatting.currencyFormatter
import io.novafoundation.nova.common.utils.formatting.formatWithFullAmount
import io.novafoundation.nova.common.utils.formatting.simpleCurrencyFormatter
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode

private val currencyFormatter = currencyFormatter()
private val simpleCurrencyFormatter = simpleCurrencyFormatter()

@Deprecated("Use FiatFormatter instead")
fun BigDecimal.formatAsCurrencyNoAbbreviation(currency: Currency): String {
    return formatCurrencySymbol(currency.symbol, currency.code) + this.formatWithFullAmount()
}

@Deprecated("Use FiatFormatter instead")
fun BigDecimal.formatAsCurrency(currency: Currency, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return formatAsCurrency(currency.symbol, currency.code, roundingMode)
}

@Deprecated("Use FiatFormatter instead")
fun BigDecimal.simpleFormatAsCurrency(currency: Currency, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return simpleFormatAsCurrency(currency.symbol, currency.code, roundingMode)
}

@Deprecated("Use FiatFormatter instead")
fun BigDecimal.formatAsCurrency(symbol: String?, code: String, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return formatCurrencySymbol(symbol, code) + currencyFormatter.format(this, roundingMode)
}

@Deprecated("Use FiatFormatter instead")
fun BigDecimal.simpleFormatAsCurrency(symbol: String?, code: String, roundingMode: RoundingMode = RoundingMode.FLOOR): String {
    return formatCurrencySymbol(symbol, code) + simpleCurrencyFormatter.format(this, roundingMode)
}

@Deprecated("Use FiatFormatter instead")
private fun formatCurrencySymbol(symbol: String?, code: String): String {
    return symbol ?: "$code "
}
