package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request

class KodadotNftsRequest(userAddress: String) {

    val query = """
        {
           nftEntities(where: {currentOwner_eq: "$userAddress"}) {
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
}
