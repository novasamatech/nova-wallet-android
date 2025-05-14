package io.novafoundation.nova.feature_pay_impl.presentation.shop

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.mapList
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.SideEffect
import io.novafoundation.nova.common.utils.stateMachine.list.dataOrNull
import io.novafoundation.nova.common.utils.stateMachine.list.states.NewPageProgressState
import io.novafoundation.nova.common.utils.stateMachine.list.toLoading
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.domain.ShopInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.ShopBrandsInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.RaiseBrand
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.adapter.items.ShopBrandRVItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class ShopViewModel(
    private val router: PayRouter,
    private val shopInteractor: ShopInteractor,
    private val shopBrandsInteractor: ShopBrandsInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val listStateMachine = PaginatedListStateMachine<RaiseBrand>(viewModelScope)

    private val brandListStateFlow = listStateMachine.state
        .toLoading()
        .shareInBackground()

    private val isWalletAvailableFlow: Flow<Boolean> = shopInteractor.observeAccountAvailableForShopping()
        .shareInBackground()

    val isNewPageLoading = listStateMachine.state
        .map { it is NewPageProgressState }

    val brandsListState = combine(brandListStateFlow, isWalletAvailableFlow) { brands, isWalletAwailable ->
        when {
            isWalletAwailable -> BrandsListState.Brands(brands.mapList { it.toUi() })
            else -> BrandsListState.UnavailableWallet
        }
    }

    val maxCashback = listStateMachine.state
        .mapNotNull { listState -> listState.dataOrNull?.maxOfOrNull(RaiseBrand::cashback) }
        .map(::formatTitle)
        .shareInBackground()

    init {
        launch {
            for (effect in listStateMachine.sideEffects) {
                when (effect) {
                    is SideEffect.LoadPage -> loadPage(effect)
                    is SideEffect.PresentError -> presentError()
                }
            }
        }
    }

    private fun RaiseBrand.toUi(): ShopBrandRVItem {
        return ShopBrandRVItem(
            id = id,
            iconUrl = iconUrl,
            name = name,
            cashback = cashback.inPercents,
            cashbackFormatted = cashback.formatPercents()
        )
    }

    fun onScrolled(lastVisiblePosition: Int) {
        listStateMachine.onEvent(PaginatedListStateMachine.Event.Scrolled(lastVisiblePosition))
    }

    fun brandClicked(brandModel: ShopBrandRVItem) = launch {
        showMessage("Not implemented")
    }

    private fun formatTitle(maxCashback: Fraction): String {
        val cashbackPercent = maxCashback.formatPercents()
        return resourceManager.getString(R.string.shop_brands_title, cashbackPercent)
    }

    private suspend fun loadPage(event: SideEffect.LoadPage) {
        shopBrandsInteractor.loadBrands(event.query, event.nextPageOffset)
            .onSuccess { dataPage ->
                listStateMachine.onEvent(PaginatedListStateMachine.Event.NewPage(dataPage, event.query))
            }.onFailure {
                Log.e(LOG_TAG, "Failed to load Raise brands")
                listStateMachine.onEvent(PaginatedListStateMachine.Event.PageError(it))
            }
    }

    private fun presentError() {
        showMessage(resourceManager.getString(R.string.common_loading_error))
    }
}
