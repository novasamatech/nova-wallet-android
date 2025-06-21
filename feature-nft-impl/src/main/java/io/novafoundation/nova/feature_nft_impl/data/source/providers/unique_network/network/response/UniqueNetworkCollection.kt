package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response

import com.google.gson.annotations.SerializedName

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
        @SerializedName("token_limit")
        val tokenLimit: Int?,
    )
}
