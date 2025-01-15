package io.novafoundation.nova.feature_dapp_impl.data.mappers

import io.novafoundation.nova.core_db.model.FavouriteDAppLocal
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp

fun mapFavouriteDAppLocalToFavouriteDApp(favouriteDAppLocal: FavouriteDAppLocal): FavouriteDApp {
    return with(favouriteDAppLocal) {
        FavouriteDApp(
            url = url,
            label = label,
            icon = icon,
            orderingIndex = orderingIndex
        )
    }
}

fun mapFavouriteDAppToFavouriteDAppLocal(favouriteDApp: FavouriteDApp): FavouriteDAppLocal {
    return with(favouriteDApp) {
        FavouriteDAppLocal(
            url = url,
            label = label,
            icon = icon,
            orderingIndex = orderingIndex
        )
    }
}
