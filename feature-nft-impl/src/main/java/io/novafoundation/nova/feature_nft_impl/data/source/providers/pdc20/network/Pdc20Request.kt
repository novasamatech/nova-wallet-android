package io.novafoundation.nova.feature_nft_impl.data.source.providers.pdc20.network

class Pdc20Request(userAddress: String, network: String) {

    val query = """
        query {
          userTokenBalances(
            where: {
              address: {
                address_eq: "$userAddress"
              }
              standard_eq: "pdc-20"
              token: { network_eq: "$network" }
            }
          ) {
            balance
            address {
              address
            }
            token {
              id
              logo
              ticker
              totalSupply
              network
            }
          }
        
          listings(
            where: {
              from: { address_eq: "$userAddress" }
              standard_eq: "pdc-20"
              token: { network_eq: "$network" }
            }
          ) {
            from {
              address
            }
        
            token {
              id
            }
        
            amount
            value
          }
        }
    """.trimIndent()
}
