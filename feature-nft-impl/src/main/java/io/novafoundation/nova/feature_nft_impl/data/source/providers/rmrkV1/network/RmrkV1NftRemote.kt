package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.network

class RmrkV1CollectionRemote(
    val max: Int,
    val name: String,
    val issuer: String,
    val metadata: String?
)

class RmrkV1NftMetadataRemote(
    val image: String,
    val description: String
)
