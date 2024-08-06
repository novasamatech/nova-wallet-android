package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import java.math.BigInteger

class ReferendumSplitAbstainVotersRequest(referendumId: BigInteger) {
    val query = """
        query {
            referendum(id:"$referendumId") {
                trackId
                castingVotings(filter: {splitAbstainVote: {isNull: false}}) {
                    nodes {
                        splitAbstainVote
                    }
                }
            }
        }
    """.trimIndent()
}
