package io.novafoundation.nova.feature_dapp_impl.domain.common

import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapDappCategoriesToDescription

fun createDAppComparator() = compareByDescending<DApp> { it.isFavourite }
    .thenBy { it.favoriteIndex }
    .thenBy { it.name }

// Build mapping in O(Metadatas + Favourites) in case of HashMap. It allows constant time access later
internal fun buildUrlToDappMapping(
    dAppMetadatas: Collection<DappMetadata>,
    favourites: Collection<FavouriteDApp>
): Map<String, DApp> {
    val favouritesByUrl = favourites.associateBy { it.url }

    return buildMap {
        val fromFavourites = favouritesByUrl.mapValues { favouriteToDApp(it.value) }
        putAll(fromFavourites)

        // overlapping metadata urls will override favourites in the map and thus use metadata for display
        val fromMetadatas = dAppMetadatas.associateBy(
            keySelector = { it.url },
            valueTransform = { dAppMetadataToDApp(it, favoriteModel = favouritesByUrl[it.url]) }
        )
        putAll(fromMetadatas)
    }
}

fun dappToFavorite(dapp: DApp, orderingIndex: Int): FavouriteDApp {
    return FavouriteDApp(
        label = dapp.name,
        icon = dapp.iconLink,
        url = dapp.url,
        orderingIndex = orderingIndex
    )
}

fun favouriteToDApp(favouriteDApp: FavouriteDApp): DApp {
    return DApp(
        name = favouriteDApp.label,
        description = favouriteDApp.url,
        iconLink = favouriteDApp.icon,
        url = favouriteDApp.url,
        isFavourite = true,
        favoriteIndex = favouriteDApp.orderingIndex
    )
}

private fun dAppMetadataToDApp(metadata: DappMetadata, favoriteModel: FavouriteDApp?): DApp {
    return DApp(
        name = metadata.name,
        description = mapDappCategoriesToDescription(metadata.categories),
        iconLink = metadata.iconLink,
        url = metadata.url,
        isFavourite = favoriteModel != null,
        favoriteIndex = favoriteModel?.orderingIndex
    )
}
