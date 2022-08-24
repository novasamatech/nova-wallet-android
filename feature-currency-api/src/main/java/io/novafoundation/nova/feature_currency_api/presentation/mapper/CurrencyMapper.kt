package io.novafoundation.nova.feature_currency_api.presentation.mapper

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.model.CurrencyModel

fun mapCurrencyToUI(currency: Currency): CurrencyModel {
    return CurrencyModel(
        id = currency.id,
        displayCode = currency.symbol ?: currency.code,
        code = currency.code,
        name = currency.name,
        isSelected = currency.selected
    )
}
