package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber

class AllHistoricalVotesRequest(address: String) {

    val query = """
        query {
           castingVotings(filter: { voter: {equalTo: "$address"}}) {
            nodes {
              referendumId
              standardVote
              splitVote
              splitAbstainVote
            }
          }
          
          delegatorVotings(filter: {delegator: {equalTo: "$address"}}) {
            nodes {
              vote
              parent {
                referendumId
                delegateId
                standardVote
              }
            }
          }
        }
    """.trimIndent()
}

class DirectHistoricalVotesRequest(address: String, recentVotesBlockThreshold: BlockNumber) {

    val query = """
        query {
          castingVotings(filter: { and: { voter: {equalTo: "$address"}, at: { greaterThanOrEqualTo: $recentVotesBlockThreshold}}}) {
            nodes {
              referendumId
              standardVote
              splitVote
              splitAbstainVote
            }
          }
        }
    """.trimIndent()
}
