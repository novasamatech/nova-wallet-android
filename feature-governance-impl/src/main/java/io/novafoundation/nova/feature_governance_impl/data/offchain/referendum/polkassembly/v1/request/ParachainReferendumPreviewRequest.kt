package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request

class ParachainReferendumPreviewRequest(networkName: String?) {
    val query = """
        query {
            posts(
                where: {type: {id: {_eq: 2}}, network: {_eq: $networkName}, onchain_link: {onchain_network_referendum_id: {_is_null: false}}}
            ) {
                title
                onchain_link {
                    onchain_referendum {
                        referendumId
                    }
                }
            }
        }
    """.trimIndent()
}
