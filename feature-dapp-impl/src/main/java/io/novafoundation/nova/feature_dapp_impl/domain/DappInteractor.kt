package io.novafoundation.nova.feature_dapp_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext

class DappInteractor(
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val favouritesDAppRepository: FavouritesDAppRepository,
    private val phishingSitesRepository: PhishingSitesRepository,
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

    suspend fun getFavoriteDApps(): List<FavouriteDApp> {
        return favouritesDAppRepository.getFavourites().sortDApps()
    }

    fun observeFavoriteDApps(): Flow<List<FavouriteDApp>> {
        return favouritesDAppRepository.observeFavourites()
            .map { it.sortDApps() }
    }

    fun observeDAppsByCategory(): Flow<GroupedList<DappCategory, DApp>> {
        return combine(
            dAppMetadataRepository.observeDAppCatalog(),
            favouritesDAppRepository.observeFavourites()
        ) { dAppCatalog, favourites ->
            val categories = dAppCatalog.categories
            val dapps = dAppCatalog.dApps

            val urlToDAppMapping = buildUrlToDappMapping(dapps, favourites)

            // Regrouping in O(Categories * Dapps)
            // Complexity should be fine for expected amount of dApps
            categories.associateWith { category ->
                dapps.filter { category in it.categories }
                    .map { urlToDAppMapping.getValue(it.url) }
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

    private inline fun CoroutineScope.runSync(crossinline sync: suspend () -> Unit): Job {
        return async { runCatching { sync() } }
    }

    suspend fun updateFavoriteDapps(favoriteDapps: List<FavouriteDApp>) {
        favouritesDAppRepository.updateFavoriteDapps(favoriteDapps)
    }

    private fun List<FavouriteDApp>.sortDApps(): List<FavouriteDApp> {
        return sortedBy { it.orderingIndex }
    }
}
