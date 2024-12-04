package io.novafoundation.nova.feature_dapp_impl.presentation.common

import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory

data class DappModel(
    val name: String,
    val description: String,
    val iconUrl: String?,
    val isFavourite: Boolean,
    val url: String
)

fun mapDappCategoriesToDescription(categories: Collection<DappCategory>) = categories.joinToString { it.name }

fun mapDappCategoryToDappCategoryModel(category: DappCategory, dApps: List<DApp>) = DappCategoryModel(
    categoryName = category.name,
    items = dApps.map { mapDappToDappModel(it) }
)

fun mapDappToDappModel(dApp: DApp) = with(dApp) {
    DappModel(
        name = name,
        description = description,
        iconUrl = iconLink,
        url = url,
        isFavourite = isFavourite
    )
}

fun mapDappModelToDApp(dApp: DappModel) = with(dApp) {
    DApp(
        name = name,
        description = description,
        iconLink = iconUrl,
        url = url,
        isFavourite = isFavourite
    )
}
