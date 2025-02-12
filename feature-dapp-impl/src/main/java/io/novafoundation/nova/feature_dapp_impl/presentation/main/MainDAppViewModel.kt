package io.novafoundation.nova.feature_dapp_impl.presentation.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.indexOfFirstOrNull
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.dappsSource
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.dappCategoryToUi
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapDAppCatalogToDAppCategoryModels
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapFavoriteDappToDappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainDAppViewModel(
    private val promotionBannersMixinFactory: PromotionBannersMixinFactory,
    private val bannerSourceFactory: BannersSourceFactory,
    private val router: DAppRouter,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val dappInteractor: DappInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val selectedWalletFlow = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    val bannersMixin = promotionBannersMixinFactory.create(bannerSourceFactory.dappsSource(), viewModelScope)

    private val favoriteDAppsFlow = dappInteractor.observeFavoriteDApps()
        .shareInBackground()

    private val groupedDAppsFlow = dappInteractor.observeDAppsByCategory()
        .inBackground()
        .share()

    private val groupedDAppsUiFlow = groupedDAppsFlow
        .map { mapDAppCatalogToDAppCategoryModels(resourceManager, it) }
        .inBackground()
        .share()

    val favoriteDAppsUIFlow = favoriteDAppsFlow
        .map { dapps -> dapps.map { mapFavoriteDappToDappModel(it) } }
        .shareInBackground()

    val shownDAppsStateFlow = groupedDAppsUiFlow
        .filterNotNull()
        .withLoading()
        .share()

    val categoriesStateFlow = groupedDAppsFlow
        .map { catalog -> catalog.categoriesWithDApps.keys.map { dappCategoryToUi(it, isSelected = false) } }
        .map { categories ->
            DAppCategoryState(
                categories = categories,
                selectedIndex = categories.indexOfFirstOrNull { it.selected }
            )
        }
        .inBackground()
        .withLoading()
        .share()

    init {
        syncDApps()
    }

    fun openCategory(categoryId: String) {
        router.openDappSearchWithCategory(categoryId)
    }

    fun accountIconClicked() {
        router.openChangeAccount()
    }

    fun dappClicked(dapp: DappModel) {
        router.openDAppBrowser(DAppBrowserPayload.Address(dapp.url))
    }

    fun searchClicked() {
        router.openDappSearch()
    }

    fun manageClicked() {
        router.openAuthorizedDApps()
    }

    private fun syncDApps() = launch {
        dappInteractor.dAppsSync()
    }

    fun openFavorites() {
        router.openDAppFavorites()
    }
}
