package io.novafoundation.nova.feature_dapp_impl.domain

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapDappCategoriesToDescription
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DAppInfo
import io.novafoundation.nova.feature_dapp_impl.util.Urls
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
        compareByDescending<DApp> { it.isFavourite }
            .thenBy { it.name }
    }

    suspend fun dAppsSync() = withContext(Dispatchers.IO) {
        val metadataSyncing = runSync { dAppMetadataRepository.syncDAppMetadatas() }
        val phishingSitesSyncing = runSync { phishingSitesRepository.syncPhishingSites() }

        joinAll(metadataSyncing, phishingSitesSyncing)
    }

    suspend fun toggleDAppFavouritesState(dApp: DApp) = withContext(Dispatchers.Default) {
        if (dApp.isFavourite) {
            favouritesDAppRepository.removeFavourite(dApp.url)
        } else {
            favouritesDAppRepository.addFavourite(dAppToFavourite(dApp))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun observeDAppsByCategory(): Flow<GroupedList<DappCategory, DApp>> {
        return combine(
            dAppMetadataRepository.observeDAppMetadatas(),
            favouritesDAppRepository.observeFavourites()
        ) { dAppMetadatas, favourites ->

            val urlToDAppMapping = buildUrlToDappMapping(dAppMetadatas, favourites)

            val favouritesCategory = DappCategory(
                id = "favourites",
                name = resourceManager.getString(R.string.dapp_favourites)
            )
            val favouritesCategoryItems = favourites.map { urlToDAppMapping.getValue(it.url) }

            val allCategories = dAppMetadatas.flatMap { it.categories }
            // Regrouping in O(Categories * Dapps)
            // Complexity should be fine for expected amount of dApps
            val derivedCategories = allCategories.associateWith { category ->
                dAppMetadatas
                    .filter { category in it.categories }
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

    private fun MutableMap<DappCategory, List<DApp>>.putCategory(category: DappCategory, items: Collection<DApp>) {
        put(category, items.sortedWith(dAppComparator))
    }

    // Build mapping in O(Metadatas + Favourites) in case of HashMap. It allows constant time access later
    @OptIn(ExperimentalStdlibApi::class)
    private fun buildUrlToDappMapping(dAppMetadatas: List<DappMetadata>, favourites: List<FavouriteDApp>): Map<String, DApp> {
        val favouritesUrls: Set<String> = favourites.mapToSet { it.url }

        return buildMap {
            val fromFavourites = favourites.associateBy(
                keySelector = { it.url },
                valueTransform = ::favouriteToDApp
            )
            putAll(fromFavourites)

            // overlapping metadata urls will override favourites in the map and thus use metadata for display
            val fromMetadatas = dAppMetadatas.associateBy(
                keySelector = { it.url },
                valueTransform = { dAppMetadataToDApp(it, isFavourite = it.url in favouritesUrls) }
            )
            putAll(fromMetadatas)
        }
    }

    private fun favouriteToDApp(favouriteDApp: FavouriteDApp): DApp {
        return DApp(
            name = favouriteDApp.label,
            description = favouriteDApp.url,
            iconLink = favouriteDApp.icon,
            url = favouriteDApp.url,
            isFavourite = true
        )
    }

    private fun dAppToFavourite(dApp: DApp): FavouriteDApp {
        return FavouriteDApp(
            url = dApp.url,
            label = dApp.name,
            icon = dApp.iconLink
        )
    }

    private fun dAppMetadataToDApp(metadata: DappMetadata, isFavourite: Boolean): DApp {
        return DApp(
            name = metadata.name,
            description = mapDappCategoriesToDescription(metadata.categories),
            iconLink = metadata.iconLink,
            url = metadata.url,
            isFavourite = isFavourite
        )
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
}
