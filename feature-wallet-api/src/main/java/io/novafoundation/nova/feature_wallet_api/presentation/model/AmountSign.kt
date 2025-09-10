package io.novafoundation.nova.feature_wallet_api.presentation.model

enum class AmountSign(val signSymbol: String) {
    NONE(""), NEGATIVE("-"), POSITIVE("+")
}
