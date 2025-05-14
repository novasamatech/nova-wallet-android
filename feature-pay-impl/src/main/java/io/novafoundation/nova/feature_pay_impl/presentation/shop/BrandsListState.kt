package io.novafoundation.nova.feature_pay_impl.presentation.shop

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.feature_pay_impl.presentation.shop.BrandsListState.Brands
import io.novafoundation.nova.feature_pay_impl.presentation.shop.BrandsListState.UnavailableWallet
import io.novafoundation.nova.feature_pay_impl.presentation.shop.adapter.items.ShopBrandRVItem

sealed interface BrandsListState {

    data object UnavailableWallet : BrandsListState

    class Brands(val brands: ExtendedLoadingState<List<ShopBrandRVItem>>) : BrandsListState
}

fun BrandsListState.getBrandsOrNull(): List<ShopBrandRVItem>? {
    return when (this) {
        is Brands -> brands.dataOrNull
        UnavailableWallet -> null
    }
}
