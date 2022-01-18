package io.novafoundation.nova.feature_dapp_impl.presentation.search

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapDappCategoriesToDescription
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchGroup
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import io.novafoundation.nova.feature_dapp_impl.domain.search.SearchDappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.search.model.DappSearchModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
class DAppSearchViewModel(
    private val router: DAppRouter,
    private val resourceManager: ResourceManager,
    private val interactor: SearchDappInteractor
) : BaseViewModel() {

    val query = MutableStateFlow("")

    val searchResults = query
        .mapLatest {
            interactor.searchDapps(it)
                .mapKeys { (searchGroup, _) -> mapSearchGroupToTextHeader(searchGroup) }
                .mapValues { (_, groupItems) -> groupItems.map(::mapSearchResultToSearchModel) }
                .toListWithHeaders()
        }
        .inBackground()
        .share()

    fun cancelClicked() {
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
                title = searchResult.metadata.name,
                description = mapDappCategoriesToDescription(searchResult.metadata.categories),
                icon = searchResult.metadata.iconLink,
                searchResult = searchResult
            )

            is DappSearchResult.Search -> DappSearchModel(
                title = searchResult.query,
                searchResult = searchResult
            )

            is DappSearchResult.Url -> DappSearchModel(
                title = searchResult.url,
                searchResult = searchResult
            )
        }
    }

    fun searchResultClicked(searchResult: DappSearchResult) {
        when (searchResult) {
            is DappSearchResult.Dapp -> router.openDAppBrowser(searchResult.metadata.url)
            is DappSearchResult.Search -> router.openDAppBrowser(searchResult.searchUrl)
            is DappSearchResult.Url -> router.openDAppBrowser(searchResult.url)
        }
    }
}
