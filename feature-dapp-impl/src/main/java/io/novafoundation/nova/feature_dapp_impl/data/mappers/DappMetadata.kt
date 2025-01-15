package io.novafoundation.nova.feature_dapp_impl.data.mappers

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_api.data.model.DappCatalog
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_impl.data.network.metadata.DappMetadataResponse

fun mapDAppMetadataResponseToDAppMetadatas(
    response: DappMetadataResponse
): DappCatalog {
    val categories = response.categories.map {
        DappCategory(
            iconUrl = it.icon,
            name = it.name,
            id = it.id
        )
    }

    val categoriesAssociatedById = categories.associateBy { it.id }

    val metadata = response.dapps.map {
        DappMetadata(
            name = it.name,
            iconLink = it.icon,
            url = it.url,
            baseUrl = Urls.normalizeUrl(it.url),
            categories = it.categories.mapNotNullTo(mutableSetOf(), categoriesAssociatedById::get)
        )
    }

    return DappCatalog(categories, metadata)
}
