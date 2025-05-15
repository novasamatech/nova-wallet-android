package io.novafoundation.nova.feature_pay_impl.data.raise.common.network

import io.novafoundation.nova.feature_pay_impl.domain.cards.ShopCardCredentials

interface CardCredentialsResponse {

    val number: CardValue?

    val pin: CardValue?

    val url: CardValue?
}

fun CardCredentialsResponse.constructCredentialsOrNull(): ShopCardCredentials? {
    // Create local variables so smart casts will work
    val link = url
    val pin = pin
    val number = number

    return when {
        number != null && pin != null -> ShopCardCredentials.Offline(number = number.toCardCredential(), pin = pin.toCardCredential())
        link != null -> ShopCardCredentials.Online(link.value)
        else -> null
    }
}
