package io.novafoundation.nova.feature_dapp_impl.domain.search

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.domain.common.buildUrlToDappMapping
import io.novafoundation.nova.feature_dapp_impl.domain.common.createDAppComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SearchDappInteractor(
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val favouritesDAppRepository: FavouritesDAppRepository,
) {

    fun categories(): Flow<List<DappCategory>> {
        return dAppMetadataRepository.observeDAppCatalog()
            .map { it.categories }
    }

    suspend fun searchDapps(query: String, categoryId: String?): GroupedList<DappSearchGroup, DappSearchResult> = withContext(Dispatchers.Default) {
        val dapps = getDapps(categoryId)

        val dappsGroupContent = dapps
            .filter { query.isEmpty() || query.lowercase() in it.name.lowercase() }
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

    private suspend fun getDapps(categoryId: String?): Collection<DApp> {
        val dApps = dAppMetadataRepository.getDAppCatalog()
            .dApps
            .filter { dapp -> categoryId == null || dapp.categories.any { it.id == categoryId } }
            .associateBy { it.url }
        val favouriteDApps = favouritesDAppRepository.getFavourites()
            .filter { categoryId == null || it.url in dApps.keys }

        val dAppByUrlMapping = buildUrlToDappMapping(dApps.values, favouriteDApps)
        return dAppByUrlMapping.values
    }
}
