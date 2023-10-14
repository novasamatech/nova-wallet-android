package io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.network

import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.models.MetadataAttribute

data class UniquesMetadata(
    val name: String?,
    val image: String?,
    val description: String?,
    val tags: List<String>?,
    val attributes: List<MetadataAttribute>?
) {
    companion object {
        val Default = UniquesMetadata(
            name = null,
            image = null,
            description = "",
            tags = null,
            attributes = null
        )
    }
}
