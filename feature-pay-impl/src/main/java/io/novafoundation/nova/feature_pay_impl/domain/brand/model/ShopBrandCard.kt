package io.novafoundation.nova.feature_pay_impl.domain.brand.model

import io.novafoundation.nova.feature_pay_impl.domain.cards.ShopCard

class ShopBrandCards(
    val brand: ShopBrand,
    val cards: List<ShopCard>
)
