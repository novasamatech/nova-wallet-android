package io.novafoundation.nova.feature_dapp_impl.domain

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DAppGroupedCatalog
import io.novafoundation.nova.feature_dapp_api.data.model.DAppUrl
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
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
import kotlin.random.Random

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

    fun observeDAppsByCategory(): Flow<DAppGroupedCatalog> {
        val shufflingSeed = Random.nextInt()

        return combine(
            dAppMetadataRepository.observeDAppCatalog(),
            favouritesDAppRepository.observeFavourites()
        ) { dAppCatalog, favourites ->
            // We use random with seed to shuffle dapps in categories the same way during updates
            val random = Random(shufflingSeed)

            val categories = dAppCatalog.categories
            val dapps = dAppCatalog.dApps

            val urlToDAppMapping = buildUrlToDappMapping(dapps, favourites)

            val popular = dAppCatalog.popular.mapNotNull { urlToDAppMapping[it] }
            val catalog = categories.associateWith { getShuffledDAppsInCategory(it, dapps, urlToDAppMapping, dAppCatalog.popular, random) }

            DAppGroupedCatalog(popular, catalog)
        }
    }

    private fun getShuffledDAppsInCategory(
        category: DappCategory,
        dapps: List<DappMetadata>,
        urlToDAppMapping: Map<String, DApp>,
        popular: List<DAppUrl>,
        shufflingSeed: Random
    ): List<DApp> {
        val categoryDApps = dapps.filter { category in it.categories }
            .map { urlToDAppMapping.getValue(it.url) }

        val popularDAppsInCategory = categoryDApps.filter { it.url in popular }
        val otherDAppsInCategory = categoryDApps.filterNot { it.url in popular }

        return popularDAppsInCategory.shuffled(shufflingSeed) + otherDAppsInCategory.shuffled(shufflingSeed)
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
