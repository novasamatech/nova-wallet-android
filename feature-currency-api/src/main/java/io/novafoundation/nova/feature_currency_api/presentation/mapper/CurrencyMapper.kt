package io.novafoundation.nova.feature_currency_api.presentation.mapper

import io.novafoundation.nova.core_db.model.CurrencyLocal
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

fun mapCurrencyFromLocal(local: CurrencyLocal): Currency {
    return with(local) {
        Currency(
            code = code,
            name = name,
            symbol = symbol,
            category = mapCurrencyCategoryFromLocal(category),
            popular = popular,
            id = id,
            coingeckoId = coingeckoId,
            selected = selected
        )
    }
}

private fun mapCurrencyCategoryFromLocal(local: CurrencyLocal.Category): Currency.Category {
    return when (local) {
        CurrencyLocal.Category.CRYPTO -> Currency.Category.CRYPTO
        CurrencyLocal.Category.FIAT -> Currency.Category.FIAT
    }
}
