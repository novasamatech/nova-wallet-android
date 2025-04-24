package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request

class KodadotMetadataRequest(metadataId: String) {

    val query = """
        {
            metadataEntityById(id: "$metadataId") {
                image
                name
                type
                description
            }
        }
    """.trimIndent()
}
