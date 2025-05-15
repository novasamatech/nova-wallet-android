package io.novafoundation.nova.feature_pay_impl.presentation.shop.common

import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopBrand
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.items.ShopBrandRVItem

fun ShopBrand.toUi(): ShopBrandRVItem {
    return ShopBrandRVItem(
        id = id,
        iconUrl = iconUrl,
        name = name,
        cashback = cashback.inPercents,
        cashbackFormatted = cashback.formatPercents()
    )
}
