package io.novafoundation.nova.feature_dapp_impl.domain.browser.addToFavourites

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.data.repository.FavouritesDAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddToFavouritesInteractor(
    private val favouritesDAppRepository: FavouritesDAppRepository,
    private val dAppMetadataRepository: DAppMetadataRepository,
) {

    suspend fun addToFavourites(url: String, label: String, icon: String?) = withContext(Dispatchers.Default) {
        val nextOrderingIndex = favouritesDAppRepository.getNextOrderingIndex()

        val favorite = FavouriteDApp(
            url = url,
            label = label,
            icon = icon,
            orderingIndex = nextOrderingIndex
        )
        favouritesDAppRepository.addFavourite(favorite)
    }

    suspend fun addToFavourites(favouriteDApp: FavouriteDApp) = withContext(Dispatchers.Default) {
        favouritesDAppRepository.addFavourite(favouriteDApp)
    }

    suspend fun resolveFavouriteDAppDisplay(url: String, suppliedLabel: String?) = withContext(Dispatchers.Default) {
        val dAppMetadataExactMatch = dAppMetadataRepository.findDAppMetadataByExactUrlMatch(url)
        val dAppMetadataBaseUrlMatches = dAppMetadataRepository.findDAppMetadatasByBaseUrlMatch(baseUrl = Urls.normalizeUrl(url))

        // we don't want to use base url match if there more than one candidate
        val dAppMetadataBaseUrlSingleMatch = dAppMetadataBaseUrlMatches.singleOrNull()

        FavouriteDApp(
            url = url,
            label = dAppMetadataExactMatch?.name ?: suppliedLabel ?: Urls.hostOf(url),
            icon = dAppMetadataExactMatch?.iconLink ?: dAppMetadataBaseUrlSingleMatch?.iconLink,
            orderingIndex = 0
        )
    }
}
