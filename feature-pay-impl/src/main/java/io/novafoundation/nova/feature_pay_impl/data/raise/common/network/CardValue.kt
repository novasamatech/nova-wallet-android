package io.novafoundation.nova.feature_pay_impl.data.raise.common.network

import io.novafoundation.nova.feature_pay_impl.domain.cards.ShopCardCredential

class CardValue(
    val raw: String,
    val value: String
)

fun CardValue.toCardCredential(): ShopCardCredential {
    return ShopCardCredential(raw = raw, value = value)
}
