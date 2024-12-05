package io.novafoundation.nova.feature_dapp_impl.data.model

import io.novafoundation.nova.feature_dapp_impl.domain.common.dappToFavorite
import io.novafoundation.nova.feature_dapp_impl.domain.common.favouriteToDApp
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapDappModelToDApp
import io.novafoundation.nova.feature_dapp_impl.presentation.common.mapDappToDappModel

data class FavouriteDApp(
    val url: String,
    val label: String,
    val icon: String?,
    val orderingIndex: Int
)

fun mapFavoriteDappToDappModel(favoriteDapp: FavouriteDApp): DappModel {
    val dapp = favouriteToDApp(favoriteDapp)
    return mapDappToDappModel(dapp)
}

fun mapDAppModelFavorite(model: DappModel, orderingIndex: Int): FavouriteDApp {
    val dapp = mapDappModelToDApp(model)
    return dappToFavorite(dapp, orderingIndex)
}
