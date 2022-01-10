package io.novafoundation.nova.feature_dapp_impl.data.mappers

import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_impl.data.network.api.DappMetadataResponse
import io.novafoundation.nova.feature_dapp_impl.util.UrlNormalizer

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
            baseUrl = UrlNormalizer.normalizeUrl(it.url),
            categories = it.categories.mapNotNullTo(mutableSetOf(), categories::get)
        )
    }
}
