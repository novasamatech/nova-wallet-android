package io.novafoundation.nova.feature_dapp_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DAppInfo
import io.novafoundation.nova.feature_dapp_impl.domain.common.buildUrlToDappMapping
import io.novafoundation.nova.feature_dapp_impl.domain.common.createDAppComparator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext

class DappInteractor(
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val favouritesDAppRepository: FavouritesDAppRepository,
    private val phishingSitesRepository: PhishingSitesRepository,
    private val resourceManager: ResourceManager,
) {

    private val dAppComparator by lazy {
        createDAppComparator()
    }

    suspend fun dAppsSync() = withContext(Dispatchers.IO) {
        val metadataSyncing = runSync { dAppMetadataRepository.syncDAppMetadatas() }
        val phishingSitesSyncing = runSync { phishingSitesRepository.syncPhishingSites() }

        joinAll(metadataSyncing, phishingSitesSyncing)
    }

    suspend fun removeDAppFromFavourites(dAppUrl: String) {
        favouritesDAppRepository.removeFavourite(dAppUrl)
    }

    suspend fun toggleDAppFavouritesState(dApp: DApp) = withContext(Dispatchers.Default) {
        if (dApp.isFavourite) {
            favouritesDAppRepository.removeFavourite(dApp.url)
        } else {
            favouritesDAppRepository.addFavourite(dAppToFavourite(dApp))
        }
    }

    fun observeDAppsByCategory(): Flow<GroupedList<DappCategory, DApp>> {
        return combine(
            dAppMetadataRepository.observeDAppCatalog(),
            favouritesDAppRepository.observeFavourites()
        ) { dAppCatalog, favourites ->
            val categories = dAppCatalog.categories
            val dapps = dAppCatalog.dApps

            val urlToDAppMapping = buildUrlToDappMapping(dapps, favourites)

            val favouritesCategory = DappCategory(
                id = "favourites",
                name = resourceManager.getString(R.string.dapp_favourites)
            )
            val favouritesCategoryItems = favourites.map { urlToDAppMapping.getValue(it.url) }

            // Regrouping in O(Categories * Dapps)
            // Complexity should be fine for expected amount of dApps
            val derivedCategories = categories.associateWith { category ->
                dapps.filter { category in it.categories }
                    .map { urlToDAppMapping.getValue(it.url) }
            }

            val categoryAll = DappCategory(
                id = "all",
                name = resourceManager.getString(R.string.common_all)
            )

            buildMap {
                putCategory(categoryAll, urlToDAppMapping.values.toList())

                if (favouritesCategoryItems.isNotEmpty()) {
                    putCategory(favouritesCategory, favouritesCategoryItems)
                }

                derivedCategories.forEach { (category, items) ->
                    putCategory(category, items)
                }
            }
        }
    }

    suspend fun getDAppInfo(dAppUrl: String): DAppInfo {
        val baseUrl = Urls.normalizeUrl(dAppUrl)

        return withContext(Dispatchers.Default) {
            DAppInfo(
                baseUrl = baseUrl,
                metadata = dAppMetadataRepository.getDAppMetadata(baseUrl)
            )
        }
    }

    private fun MutableMap<DappCategory, List<DApp>>.putCategory(category: DappCategory, items: Collection<DApp>) {
        put(category, items.sortedWith(dAppComparator))
    }

    private fun dAppToFavourite(dApp: DApp): FavouriteDApp {
        return FavouriteDApp(
            url = dApp.url,
            label = dApp.name,
            icon = dApp.iconLink
        )
    }

    private inline fun CoroutineScope.runSync(crossinline sync: suspend () -> Unit): Job {
        return async { runCatching { sync() } }
    }
}
