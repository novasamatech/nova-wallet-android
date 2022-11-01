package io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.request

import java.math.BigInteger

class ReferendumDetailsRequest(id: BigInteger) {
    val query = """
        query {
            posts(
                where: {id: {_eq: $id}}
            ) {
                title
                content
                author {
                    username
                }
                onchain_link {
                    onchain_referendum{
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
