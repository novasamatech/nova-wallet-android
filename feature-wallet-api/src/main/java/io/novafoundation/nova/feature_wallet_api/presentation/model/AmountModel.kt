package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.map

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

fun MaskableModel<AmountModel>.maskableToken() = map { it.token }
fun MaskableModel<AmountModel>.maskableFiat() = map { it.fiat }
