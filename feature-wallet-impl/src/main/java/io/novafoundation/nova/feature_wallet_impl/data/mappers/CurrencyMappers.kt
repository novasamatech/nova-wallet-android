package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.core_db.model.CurrencyLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_impl.data.datasource.CurrencyRemote


fun mapRemoteCurrencyToLocal(remote: CurrencyRemote, selected: Boolean): CurrencyLocal {
    return with(remote) {
        CurrencyLocal(
            code = code,
            name = name,
            symbol = symbol,
            category = mapRemoteCurrencyCategoryToLocal(category),
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

private fun mapRemoteCurrencyCategoryToLocal(remote: String): CurrencyLocal.Category {
    return when(remote) {
        "fiat" -> CurrencyLocal.Category.FIAT
        "crypto" -> CurrencyLocal.Category.CRYPTO
        else -> throw IllegalArgumentException("Unknown currency category: $remote")
    }
}
