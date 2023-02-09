package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request

import java.math.BigInteger

class ReferendumVotersRequest(referendumId: BigInteger, isAye: Boolean) {
    val query = """
        query {
            castingVotings(filter:{referendumId:{equalTo:"$referendumId"}, standardVote: {contains: {aye: $isAye}}}) {
                nodes {
                    voter
             		standardVote
                    delegateId
                    delegatorVotes {
                        nodes {
                            delegator
                            vote
                        }
                    }
                }
            }
        }
    """.trimIndent()
}
