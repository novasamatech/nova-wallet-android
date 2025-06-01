package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response

data class UniqueNetworkCollection(
    val collectionId: Int,
    val name: String?,
    val description: String?,
    val coverImage: CoverImage?,
    val owner: String,
    val propertiesMap: Map<String, Property>?
) {
    data class CoverImage(
        val url: String?
    )

    data class Property(
        val key: String,
        val value: String
    )
}
