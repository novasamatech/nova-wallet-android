package io.novafoundation.nova.feature_currency_impl.data.datasource

import io.novafoundation.nova.core_db.model.CurrencyLocal

class CurrencyRemote(
    val code: String,
    val name: String,
    val symbol: String?,
    val category: String,
    val popular: Boolean,
    val id: Int,
    val coingeckoId: String,
)

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

private fun mapRemoteCurrencyCategoryToLocal(remote: String): CurrencyLocal.Category {
    return when (remote) {
        "fiat" -> CurrencyLocal.Category.FIAT
        "crypto" -> CurrencyLocal.Category.CRYPTO
        else -> throw IllegalArgumentException("Unknown currency category: $remote")
    }
}
