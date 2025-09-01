package io.novafoundation.nova.feature_nft_impl.data.source.providers.unique_network.network.response

data class UniqueNetworkPaginatedResponse<T>(
    val items: List<T>,
    val count: Int
)
