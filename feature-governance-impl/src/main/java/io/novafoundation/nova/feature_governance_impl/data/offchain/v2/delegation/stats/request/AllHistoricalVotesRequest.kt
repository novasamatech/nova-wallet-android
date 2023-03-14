package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request

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
                delegate {
                  accountId
                }
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
