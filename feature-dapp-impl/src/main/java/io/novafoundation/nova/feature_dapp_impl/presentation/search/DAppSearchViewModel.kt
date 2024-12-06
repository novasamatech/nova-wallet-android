package io.novafoundation.nova.feature_dapp_impl.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchGroup
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import io.novafoundation.nova.feature_dapp_impl.domain.search.SearchDappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.search.model.DappSearchModel
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DAppSearchViewModel(
    private val router: DAppRouter,
    private val resourceManager: ResourceManager,
    private val interactor: SearchDappInteractor,
    private val payload: SearchPayload,
    private val dAppSearchResponder: DAppSearchResponder,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val appLinksProvider: AppLinksProvider,
    private val browserTabService: BrowserTabService
) : BaseViewModel() {

    val dAppNotInCatalogWarning = actionAwaitableMixinFactory.confirmingAction<DappUnknownWarningModel>()

    val query = MutableStateFlow(payload.initialUrl.orEmpty())

    private val _selectQueryTextEvent = MutableLiveData<Event<Unit>>()
    val selectQueryTextEvent: LiveData<Event<Unit>> = _selectQueryTextEvent

    val searchResults = query
        .mapLatest {
            interactor.searchDapps(it)
                .mapKeys { (searchGroup, _) -> mapSearchGroupToTextHeader(searchGroup) }
                .mapValues { (_, groupItems) -> groupItems.map(::mapSearchResultToSearchModel) }
                .toListWithHeaders()
        }
        .inBackground()
        .share()

    init {
        if (!payload.initialUrl.isNullOrEmpty()) {
            _selectQueryTextEvent.sendEvent()
        }
    }

    fun cancelClicked() {
        if (shouldReportResult()) {
            dAppSearchResponder.respond(DAppSearchCommunicator.Response.Cancel)
        }

        router.back()
    }

    private fun mapSearchGroupToTextHeader(searchGroup: DappSearchGroup): TextHeader {
        val content = when (searchGroup) {
            DappSearchGroup.DAPPS -> resourceManager.getString(R.string.dapp_dapps)
            DappSearchGroup.SEARCH -> resourceManager.getString(R.string.common_search)
        }

        return TextHeader(content)
    }

    private fun mapSearchResultToSearchModel(searchResult: DappSearchResult): DappSearchModel {
        return when (searchResult) {
            is DappSearchResult.Dapp -> DappSearchModel(
                title = searchResult.dapp.name,
                description = searchResult.dapp.description,
                icon = searchResult.dapp.iconLink,
                searchResult = searchResult,
                actionIcon = R.drawable.ic_favorite_heart_filled_20.takeIf { searchResult.dapp.isFavourite }
            )

            is DappSearchResult.Search -> DappSearchModel(
                title = searchResult.query,
                searchResult = searchResult,
                actionIcon = null
            )

            is DappSearchResult.Url -> DappSearchModel(
                title = searchResult.url,
                searchResult = searchResult,
                actionIcon = null
            )
        }
    }

    fun searchResultClicked(searchResult: DappSearchResult) {
        launch {
            val newUrl = when (searchResult) {
                is DappSearchResult.Dapp -> searchResult.dapp.url
                is DappSearchResult.Search -> searchResult.searchUrl
                is DappSearchResult.Url -> searchResult.url
            }

            if (searchResult !is DappSearchResult.Dapp) {
                dAppNotInCatalogWarning.awaitAction(DappUnknownWarningModel(appLinksProvider.email))
            }

            when (payload.request) {
                SearchPayload.Request.GO_TO_URL -> {
                    dAppSearchResponder.respond(DAppSearchCommunicator.Response.NewUrl(newUrl))
                    router.finishDappSearch()
                }

                SearchPayload.Request.OPEN_NEW_URL -> router.openDAppBrowser(DAppBrowserPayload.Address(newUrl))
            }
        }
    }

    private fun shouldReportResult() = when (payload.request) {
        SearchPayload.Request.GO_TO_URL -> true

        SearchPayload.Request.OPEN_NEW_URL -> false
    }
}
