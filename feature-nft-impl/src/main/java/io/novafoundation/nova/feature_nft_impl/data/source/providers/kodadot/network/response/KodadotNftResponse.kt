package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response

class KodadotNftResponse(
    val nftEntities: List<KodadotNftRemote>
)

class KodadotNftRemote(
    val id: String,
    val image: String?,
    val metadata: String?,
    val name: String?,
    val price: String?,
    val sn: String?,
    val currentOwner: String,
    val collection: Collection?
) {

    class Collection(
        val id: String,
        val max: String
    )
}
