package io.novafoundation.nova.feature_currency_api.presentation.model

class CurrencyRemote(
    val code: String,
    val name: String,
    val symbol: String?,
    val category: String,
    val popular: Boolean,
    val id: Int,
    val coingeckoId: String,
)
