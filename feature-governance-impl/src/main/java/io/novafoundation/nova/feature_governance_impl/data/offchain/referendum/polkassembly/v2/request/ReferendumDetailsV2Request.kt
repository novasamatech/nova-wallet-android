package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.request

import java.math.BigInteger

class ReferendumDetailsV2Request(id: BigInteger) {
    val query = """
        query {
            posts(
                where: {onchain_link: {onchain_referendumv2_id: {_eq: $id}}}
            ) {
                title
                content
                author {
                    username
                }
                onchain_link {
                    onchain_referendumv2 {
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
