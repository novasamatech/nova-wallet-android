package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request

import java.math.BigInteger

class ParachainReferendumDetailsRequest(network: String, id: BigInteger) {
    val query = """
        query {
            posts(
                where: {onchain_link: {onchain_network_referendum_id: {_eq: "${network}_$id"}}}
            ) {
                title
                content
                author {
                    username
                }
                onchain_link {
                    onchain_referendum {
                        referendumStatus {
                            blockNumber {
                                startDateTime
                                number
                            }
                            status
                        }
                    }
                }
            }
        }
    """.trimIndent()
}
