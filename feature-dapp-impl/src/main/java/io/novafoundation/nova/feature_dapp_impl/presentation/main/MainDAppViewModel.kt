package io.novafoundation.nova.feature_dapp_impl.presentation.main

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.indexOfFirstOrNull
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapDappModelToDApp
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapDappToDappModel
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.RemoveFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryModel
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val INITIAL_SELECTED_CATEGORY_ID = "all"

class MainDAppViewModel(
    private val router: DAppRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val dappInteractor: DappInteractor,
) : BaseViewModel(), Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val removeFavouriteConfirmationAwaitable = actionAwaitableMixinFactory.confirmingAction<RemoveFavouritesPayload>()

    val selectedWalletFlow = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val groupedDAppsFlow = dappInteractor.observeDAppsByCategory()
        .inBackground()
        .share()

    private val groupedDAppsUiFlow = groupedDAppsFlow
        .map {
            it
                .mapValues { (_, dapps) -> dapps.map(::mapDappToDappModel) }
                .mapKeys { (category, _) -> category.id }
        }
        .inBackground()
        .share()

    private val selectedCategoryId = MutableStateFlow(INITIAL_SELECTED_CATEGORY_ID)

    private val shownDappsFlow = combine(groupedDAppsUiFlow, selectedCategoryId) { grouping, categoryId ->
        grouping[categoryId]
    }
        .share()

    val shownDAppsStateFlow = shownDappsFlow
        .filterNotNull()
        .withLoading()
        .share()

    private val categoriesFlow = combine(groupedDAppsFlow, selectedCategoryId) { grouping, categoryId ->
        grouping.keys.map { dappCategoryToUi(it, isSelected = it.id == categoryId) }
    }
        .distinctUntilChanged()
        .inBackground()
        .share()

    val categoriesStateFlow = categoriesFlow
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

        watchInvalidSelectedCategory()
    }

    fun categorySelected(categoryId: String) = launch {
        selectedCategoryId.emit(categoryId)
    }

    fun accountIconClicked() {
        router.openChangeAccount()
    }

    fun dappClicked(dapp: DappModel) {
        router.openDAppBrowser(dapp.url)
    }

    fun searchClicked() {
        router.openDappSearch()
    }

    fun dappFavouriteClicked(item: DappModel) = launch {
        val dApp = mapDappModelToDApp(item)

        if (item.isFavourite) {
            removeFavouriteConfirmationAwaitable.awaitAction(item.name)
        }

        dappInteractor.toggleDAppFavouritesState(dApp)
    }

    fun manageClicked() {
        router.openAuthorizedDApps()
    }

    private fun watchInvalidSelectedCategory() = shownDappsFlow.onEach {
        // cannot find selected category in current grouping
        if (it == null) selectedCategoryId.value = INITIAL_SELECTED_CATEGORY_ID
    }.launchIn(this)

    private fun syncDApps() = launch {
        dappInteractor.dAppsSync()
    }

    private fun dappCategoryToUi(dappCategory: DappCategory, isSelected: Boolean): DAppCategoryModel {
        return DAppCategoryModel(
            id = dappCategory.id,
            name = dappCategory.name,
            selected = isSelected
        )
    }
}
