package io.novafoundation.nova.feature_dapp_impl.domain.browser.addToFavourites

import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import io.novafoundation.nova.feature_dapp_impl.util.Urls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddToFavouritesInteractor(
    private val favouritesDAppRepository: FavouritesDAppRepository,
    private val dAppMetadataRepository: DAppMetadataRepository,
) {

    suspend fun addToFavourites(favouriteDApp: FavouriteDApp) = withContext(Dispatchers.Default) {
        favouritesDAppRepository.addFavourite(favouriteDApp)
    }

    /**
     * Tries to resolve given url without breaking user expectations, that is
     * - Url should be the same as supplied
     * - Label should be either the name of known dApp by exact url match OR supplied label OR host of url
     * - Icon may be taken both from known dApp by exact match OR known dApp by base url match
     *
     * We do not allow label to be taken from known dApp by base url match since it may be confusing for a user to see the same label as for exact-matched entry
     */
    suspend fun resolveFavouriteDAppDisplay(url: String, suppliedLabel: String?) = withContext(Dispatchers.Default) {
        val dAppMetadataExactMatch = dAppMetadataRepository.findDAppMetadataByExactUrlMatch(url)
        val dAppMetadataBaseUrlMatch = dAppMetadataRepository.getDAppMetadata(baseUrl = Urls.normalizeUrl(url))

        FavouriteDApp(
            url = url,
            label = dAppMetadataExactMatch?.name ?: suppliedLabel ?: Urls.hostOf(url),
            icon = dAppMetadataExactMatch?.iconLink ?: dAppMetadataBaseUrlMatch?.iconLink
        )
    }
}
