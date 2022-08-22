package io.novafoundation.nova.feature_wallet_api.presentation.model

class CurrencyModel(
    val id: Int,
    val sign: String,
    val code: String,
    val name: String,
    val isSelected: Boolean
)
