package io.novafoundation.nova.feature_pay_impl.domain.cards

import io.novafoundation.nova.common.domain.model.Timestamp
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopBrand
import io.novafoundation.nova.feature_wallet_api.domain.model.FiatAmount

class ShopCard(
    val fiatBalance: FiatAmount,
    val id: String,
    val expiration: Timestamp?,
    val credentials: ShopCardCredentials
) : Identifiable {

    override val identifier: String = id
}

sealed class ShopCardCredentials {

    class Offline(val number: ShopCardCredential, val pin: ShopCardCredential) : ShopCardCredentials()

    class Online(val link: String) : ShopCardCredentials()
}

class ShopCardCredential(val raw: String, val value: String)

class BrandedShopCard(
    val card: ShopCard,
    val brand: ShopBrand,
)
