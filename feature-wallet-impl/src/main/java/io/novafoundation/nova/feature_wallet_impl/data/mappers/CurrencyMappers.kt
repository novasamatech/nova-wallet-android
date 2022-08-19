package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.core_db.model.CurrencyLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_impl.data.datasource.CurrencyRemote

fun mapCurrencyFromRemote(remote: CurrencyRemote, selected: Boolean): Currency {
    return with(remote) {
        Currency(
            code = code,
            name = name,
            symbol = symbol,
            category = mapCurrencyCategoryFromRemote(category),
            popular = popular,
            id = id,
            coingeckoId = coingeckoId,
            selected = selected,
        )
    }
}

fun mapCurrencyToLocal(currency: Currency): CurrencyLocal {
    return with(currency) {
        CurrencyLocal(
            code = code,
            name = name,
            symbol = symbol,
            category = mapCurrencyCategoryToLocal(category),
            popular = popular,
            id = id,
            coingeckoId = coingeckoId,
            selected = selected
        )
    }
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

private fun mapCurrencyCategoryToLocal(category: Currency.Category): CurrencyLocal.Category {
    return when (category) {
        Currency.Category.CRYPTO -> CurrencyLocal.Category.CRYPTO
        Currency.Category.FIAT -> CurrencyLocal.Category.FIAT
    }
}

private fun mapCurrencyCategoryFromLocal(local: CurrencyLocal.Category): Currency.Category {
    return when (local) {
        CurrencyLocal.Category.CRYPTO -> Currency.Category.CRYPTO
        CurrencyLocal.Category.FIAT -> Currency.Category.FIAT
    }
}

private fun mapCurrencyCategoryFromRemote(remote: String): Currency.Category {
    return when(remote) {
        "fiat" -> Currency.Category.FIAT
        "crypto" -> Currency.Category.CRYPTO
        else -> throw IllegalArgumentException("Unknown currency category: $remote")
    }
}
