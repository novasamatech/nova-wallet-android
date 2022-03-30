package io.novafoundation.nova.feature_dapp_impl.data.mappers

import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_impl.data.network.metadata.DappMetadataResponse
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.util.Urls

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
            categories = it.categories.mapNotNullTo(mutableSetOf(), categories::get)
        )
    }
}

fun mapDappCategoriesToDescription(categories: Collection<DappCategory>) = categories.joinToString { it.name }

fun mapDappMetadataToDappModel(dappMetadata: DappMetadata) = with(dappMetadata) {
    DappModel(
        name = name,
        description = mapDappCategoriesToDescription(categories),
        iconUrl = iconLink,
        url = url,
    )
}
