package io.novafoundation.nova.feature_wallet_api.presentation.model

data class AmountModel(
    val token: CharSequence,
    val fiat: CharSequence?
) {

    // Override it since SpannableString is not equals by content
    override fun equals(other: Any?): Boolean {
        return other is AmountModel &&
            other.token.toString() == token.toString() &&
            other.fiat?.toString() == fiat?.toString()
    }
}
