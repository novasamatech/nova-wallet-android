package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.domain.common.buildUrlToDappMapping
import io.novafoundation.nova.feature_dapp_impl.domain.common.createDAppComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchDappInteractor(
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val favouritesDAppRepository: FavouritesDAppRepository,
) {

    suspend fun searchDapps(query: String): GroupedList<DappSearchGroup, DappSearchResult> = withContext(Dispatchers.Default) {
        val dAppMetadatas = dAppMetadataRepository.getDAppMetadatas()
        val favouriteDApps = favouritesDAppRepository.getFavourites()

        val dAppByUrlMapping = buildUrlToDappMapping(dAppMetadatas, favouriteDApps)
        val allDApps = dAppByUrlMapping.values

        val dappsGroupContent = allDApps.filter { query.isEmpty() || query.lowercase() in it.name.lowercase() }
            .sortedWith(createDAppComparator())
            .map(DappSearchResult::Dapp)

        val searchGroupContent = when {
            query.isEmpty() -> null
            Urls.isValidWebUrl(query) -> DappSearchResult.Url(Urls.ensureHttpsProtocol(query))
            else -> DappSearchResult.Search(query, searchUrlFor(query))
        }

        buildMap {
            searchGroupContent?.let {
                put(DappSearchGroup.SEARCH, listOf(searchGroupContent))
            }

            if (dappsGroupContent.isNotEmpty()) {
                put(DappSearchGroup.DAPPS, dappsGroupContent)
            }
        }
    }

    private fun searchUrlFor(query: String): String = "https://duckduckgo.com/?q=$query"
}
