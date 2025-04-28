package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request

class KodadotCollectionRequest(collectionId: String) {

    val query = """
        {
            collectionEntityById(id: "$collectionId") {
                name
                image
                issuer
            }
        }
    """.trimIndent()
}
