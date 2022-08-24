package io.novafoundation.nova.feature_currency_api.presentation.model

data class CurrencyModel(
    val id: Int,
    val displayCode: String,
    val code: String,
    val name: String,
    val isSelected: Boolean
)
