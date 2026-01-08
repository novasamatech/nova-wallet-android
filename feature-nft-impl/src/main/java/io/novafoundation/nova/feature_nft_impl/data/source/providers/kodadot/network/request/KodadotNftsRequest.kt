package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request

class KodadotNftsRequest(userAddress: String) {

    val query = """
        query nftListByOwner(${'$'}id: String!) {
           nftEntities(where: {currentOwner_eq: ${'$'}id, burned_eq: false}) {
               id
               image
               metadata
               name
               price
               sn
               currentOwner
               collection {
                 id
                 max
                }
            }
        }
    """.trimIndent()

    val variables = mapOf("id" to userAddress)
}
