package io.novafoundation.nova.feature_dapp_impl.domain.common

import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapDappCategoriesToDescription

fun createDAppComparator() = compareByDescending<DApp> { it.isFavourite }
    .thenBy { it.name }

// Build mapping in O(Metadatas + Favourites) in case of HashMap. It allows constant time access later
internal fun buildUrlToDappMapping(
    dAppMetadatas: Collection<DappMetadata>,
    favourites: Collection<FavouriteDApp>
): Map<String, DApp> {
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

fun favouriteToDApp(favouriteDApp: FavouriteDApp): DApp {
    return DApp(
        name = favouriteDApp.label,
        description = favouriteDApp.url,
        iconLink = favouriteDApp.icon,
        url = favouriteDApp.url,
        isFavourite = true
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
