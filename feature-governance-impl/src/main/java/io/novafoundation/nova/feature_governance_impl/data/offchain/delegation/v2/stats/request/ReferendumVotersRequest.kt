package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.common.data.network.subquery.SubqueryExpressions.anyOf
import java.math.BigInteger

class ReferendumVotersRequest(referendumId: BigInteger, isAye: Boolean) {
    val query = """
        query {
            castingVotings(filter:{referendumId:{equalTo:"$referendumId"}, ${voteTypeFilter(isAye)}}) {
                nodes {
                    voter
             		standardVote
                    splitVote
                    splitAbstainVote
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

    private fun voteTypeFilter(isAye: Boolean): String {
        return anyOf(standardVoteFilter(isAye), splitVoteFilter(), splitAbstainVote())
    }

    // we cannot filter JSON field by checking splitVote.ayeAmount > 0 so it should be done after request
    private fun splitVoteFilter(): String {
        return "splitVote: {isNull: false}"
    }

    private fun splitAbstainVote(): String {
        return "splitAbstainVote: {isNull: false}"
    }

    private fun standardVoteFilter(isAye: Boolean): String {
        return "standardVote: {contains: {aye: $isAye}}"
    }
}
