package io.novafoundation.nova.feature_pay_impl.presentation.shop.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.mapList
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.utils.stateMachine.list.dataOrNull
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.domain.ShopInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.ShopBrandsInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopBrand
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.BrandsPaginationMixinFactory
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.toUi
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.items.ShopBrandRVItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ShopViewModel(
    private val router: PayRouter,
    private val shopInteractor: ShopInteractor,
    private val resourceManager: ResourceManager,
    private val paginationMixinFactory: BrandsPaginationMixinFactory,
    private val shopBrandsInteractor: ShopBrandsInteractor,
) : BaseViewModel() {

    val paginationMixin = paginationMixinFactory.create(this)

    private val isWalletAvailableFlow: Flow<Boolean> = shopInteractor.observeAccountAvailableForShopping()
        .shareInBackground()

    val brandsListState = combine(paginationMixin.itemsFlow, isWalletAvailableFlow) { brands, isWalletAwailable ->
        when {
            isWalletAwailable -> BrandsListState.Brands(brands.mapList { it.toUi() })
            else -> BrandsListState.UnavailableWallet
        }
    }

    val maxCashback = paginationMixin.stateFlow
        .mapNotNull { listState -> listState.dataOrNull?.maxOfOrNull(ShopBrand::cashback) }
        .map(::formatTitle)
        .shareInBackground()

    val purchasedCardsState = shopBrandsInteractor.purchasedCards()
        .map {
            when {
                it.isEmpty() -> PurchasedCardsState.Empty
                else -> PurchasedCardsState.Content(it.size)
            }
        }

    init {
        paginationMixin.init()

        launch {
            shopBrandsInteractor.syncPurchasedCards()
        }

        paginationMixin.errorFlow
            .onEach { showMessage(it) }
            .launchIn(this)
    }

    fun onSearchClick() {
        router.openShopSearch()
    }

    fun onScrolled(lastVisiblePosition: Int) {
        paginationMixin.onScroll(lastVisiblePosition)
    }

    fun brandClicked(brandModel: ShopBrandRVItem) = launch {
        showMessage("Not implemented")
    }

    fun purchasesClicked() {

    }

    private fun formatTitle(maxCashback: Fraction): String {
        val cashbackPercent = maxCashback.formatPercents()
        return resourceManager.getString(R.string.shop_brands_title, cashbackPercent)
    }
}
