package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response

data class UniqueNetworkNft(
    val key: String,
    val collectionId: Int,
    val tokenId: Int,
    val owner: String,
    val topmostOwner: String,
    val isBundle: Boolean,
    val image: String?,
    val name: String?,
    val description: String?,
    val attributes: List<Attribute>?,
    val propertiesMap: Map<String, Property>?,
    val isBurned: Boolean
) {
    data class Attribute(
        val trait_type: String,
        val value: String
    )

    data class Property(
        val key: String,
        val value: String
    )
}
