package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request

import java.math.BigInteger

class ReferendumDetailsRequest(id: BigInteger) {
    val query = """
        query {
            posts(
                where: {onchain_link: {onchain_referendum_id: {_eq: $id}}}
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
                    proposer_address
                }
            }
        }
    """.trimIndent()
}
