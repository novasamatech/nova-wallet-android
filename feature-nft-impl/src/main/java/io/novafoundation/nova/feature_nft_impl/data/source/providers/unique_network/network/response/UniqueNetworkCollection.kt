package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response

data class UniqueNetworkCollection(
    val collectionId: Int,
    val name: String?,
    val coverImage: CoverImage?,
    val lastTokenId: Int?,
    val limits: Limits?
) {
    data class CoverImage(
        val url: String?
    )

    data class Limits(
        val token_limit: Int?,
    )
}
