package io.novafoundation.nova.feature_dapp_impl.presentation.main

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.feature_banners_impl.presentation.banner.PromotionBannersMixinFactory
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.indexOfFirstOrNull
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.dappCategoryToUi
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapDappCategoryToDappCategoryModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapFavoriteDappToDappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainDAppViewModel(
    private val promotionBannersMixinFactory: io.novafoundation.nova.feature_banners_impl.presentation.banner.PromotionBannersMixinFactory,
    private val promotionBannersInteractor: io.novafoundation.nova.feature_banners_impl.domain.PromotionBannersInteractor,
    private val router: DAppRouter,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val dappInteractor: DappInteractor,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val selectedWalletFlow = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    val bannersMixin = promotionBannersMixinFactory.create { promotionBannersInteractor.getDAppBanners() }

    private val favoriteDAppsFlow = dappInteractor.observeFavoriteDApps()
        .shareInBackground()

    private val groupedDAppsFlow = dappInteractor.observeDAppsByCategory()
        .inBackground()
        .share()

    private val groupedDAppsUiFlow = groupedDAppsFlow
        .map {
            it.map { (category, dapps) -> mapDappCategoryToDappCategoryModel(category, dapps) }
        }
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
        .map { catalog -> catalog.keys.map { dappCategoryToUi(it, isSelected = false) } }
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
