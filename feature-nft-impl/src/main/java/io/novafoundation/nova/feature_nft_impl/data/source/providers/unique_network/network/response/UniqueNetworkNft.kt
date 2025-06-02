package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response

data class UniqueNetworkNft(
    val key: String,
    val collectionId: Int,
    val tokenId: Int,
    val image: String?,
    val name: String?,
)
