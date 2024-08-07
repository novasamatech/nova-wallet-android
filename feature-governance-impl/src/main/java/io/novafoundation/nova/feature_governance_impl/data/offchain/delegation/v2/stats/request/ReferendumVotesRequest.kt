package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import java.math.BigInteger

class ReferendumVotesRequest(referendumId: BigInteger) {
    val query = """
        query {
            referendum(id:"$referendumId") {
                trackId
                castingVotings {
                    nodes {
                        splitVote
                        splitAbstainVote
                        standardVote
                        delegatorVotes {
                            nodes {
                                vote
                            }
                        }
                    }
                }
            }
        }
    """.trimIndent()
}
