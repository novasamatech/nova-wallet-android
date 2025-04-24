package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response

class KodadotCollectionResponse(
    val collectionEntityById: KodadotCollectionRemote?
)

class KodadotCollectionRemote(
    val name: String?,
    val image: String?,
    val issuer: String?
)
