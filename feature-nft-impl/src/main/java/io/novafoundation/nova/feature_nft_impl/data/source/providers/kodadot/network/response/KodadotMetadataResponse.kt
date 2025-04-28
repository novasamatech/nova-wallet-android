package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response

class KodadotMetadataResponse(
    val metadataEntityById: KodadotMetadataRemote?
)

class KodadotMetadataRemote(
    val name: String?,
    val description: String?,
    val type: String?,
    val image: String?
)
