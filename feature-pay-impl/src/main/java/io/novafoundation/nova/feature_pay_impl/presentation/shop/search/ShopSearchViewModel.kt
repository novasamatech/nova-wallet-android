package io.novafoundation.nova.feature_pay_impl.presentation.shop.search

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.mapList
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.BrandsPaginationMixinFactory
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.toUi
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.adapter.items.ShopBrandRVItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ShopSearchViewModel(
    private val router: PayRouter,
    private val paginationMixinFactory: BrandsPaginationMixinFactory
) : BaseViewModel() {

    val paginationMixin = paginationMixinFactory.create(this)

    val brandsListState = paginationMixin.itemsFlow.map { brands ->
        brands.mapList { it.toUi() }
    }

    init {
        paginationMixin.init()

        paginationMixin.errorFlow
            .onEach { showMessage(it) }
            .launchIn(this)
    }

    fun onScrolled(lastVisiblePosition: Int) {
        paginationMixin.onScroll(lastVisiblePosition)
    }

    fun brandClicked(brandModel: ShopBrandRVItem) = launch {
        showMessage("Not implemented")
    }

    fun backClicked() {
        router.back()
    }
}
