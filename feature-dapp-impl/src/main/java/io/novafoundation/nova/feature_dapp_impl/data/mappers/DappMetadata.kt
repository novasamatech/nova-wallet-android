package io.novafoundation.nova.feature_dapp_impl.data.mappers

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.model.DApp
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_impl.data.network.metadata.DappMetadataResponse
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel

fun mapDAppMetadataResponseToDAppMetadatas(
    response: DappMetadataResponse
): List<DappMetadata> {
    val categories = response.categories.map {
        DappCategory(
            name = it.name,
            id = it.id
        )
    }.associateBy { it.id }

    return response.dapps.map {
        DappMetadata(
            name = it.name,
            iconLink = it.icon,
            url = it.url,
            baseUrl = Urls.normalizeUrl(it.url),
            categories = it.categories.mapNotNullTo(mutableSetOf(), categories::get),
            desktopOnly = it.desktopOnly ?: false
        )
    }
}

fun mapDappCategoriesToDescription(categories: Collection<DappCategory>) = categories.joinToString { it.name }

fun mapDappToDappModel(dApp: DApp) = with(dApp) {
    DappModel(
        name = name,
        description = description,
        iconUrl = iconLink,
        url = url,
        isFavourite = isFavourite,
        desktopOnly = desktopOnly
    )
}

fun mapDappModelToDApp(dApp: DappModel) = with(dApp) {
    DApp(
        name = name,
        description = description,
        iconLink = iconUrl,
        url = url,
        isFavourite = isFavourite,
        desktopOnly = desktopOnly
    )
}
