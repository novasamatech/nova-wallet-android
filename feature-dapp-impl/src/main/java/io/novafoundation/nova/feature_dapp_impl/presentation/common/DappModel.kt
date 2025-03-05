package io.novafoundation.nova.feature_dapp_impl.presentation.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DAppGroupedCatalog
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.data.model.FavouriteDApp
import io.novafoundation.nova.feature_dapp_impl.domain.common.dappToFavorite
import io.novafoundation.nova.feature_dapp_impl.domain.common.favouriteToDApp

data class DappModel(
    val name: String,
    val description: String,
    val iconUrl: String?,
    val isFavourite: Boolean,
    val favoriteIndex: Int?,
    val url: String
)

fun mapDappCategoriesToDescription(categories: Collection<DappCategory>) = categories.joinToString { it.name }

fun mapDAppCatalogToDAppCategoryModels(resourceManager: ResourceManager, dappCatalog: DAppGroupedCatalog): List<DappCategoryModel> {
    val popular = mapDappCategoryToDappCategoryModel(resourceManager.getString(R.string.popular_dapps_title), dappCatalog.popular)
    val categories = dappCatalog.categoriesWithDApps.map { (category, dapps) -> mapDappCategoryToDappCategoryModel(category.name, dapps) }

    return listOf(popular) + categories
}

fun mapDappCategoryToDappCategoryModel(categoryName: String, dApps: List<DApp>) = DappCategoryModel(
    categoryName = categoryName,
    items = dApps.map { mapDappToDappModel(it) }
)

fun mapDappToDappModel(dApp: DApp) = with(dApp) {
    DappModel(
        name = name,
        description = description,
        iconUrl = iconLink,
        url = url,
        isFavourite = isFavourite,
        favoriteIndex = favoriteIndex
    )
}

fun mapDappModelToDApp(dApp: DappModel) = with(dApp) {
    DApp(
        name = name,
        description = description,
        iconLink = iconUrl,
        url = url,
        isFavourite = isFavourite,
        favoriteIndex = favoriteIndex
    )
}

fun mapFavoriteDappToDappModel(favoriteDapp: FavouriteDApp): DappModel {
    val dapp = favouriteToDApp(favoriteDapp)
    return mapDappToDappModel(dapp)
}

fun mapDAppModelToFavorite(model: DappModel, orderingIndex: Int): FavouriteDApp {
    val dapp = mapDappModelToDApp(model)
    return dappToFavorite(dapp, orderingIndex)
}
