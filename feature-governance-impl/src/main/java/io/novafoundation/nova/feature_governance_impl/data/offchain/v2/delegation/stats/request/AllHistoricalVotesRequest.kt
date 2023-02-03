package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId

class AllHistoricalVotesRequest(address: String) {

    val query = """
        query {
           castingVotings(filter: { voter: {equalTo: "$address"}}) {
            nodes {
              referendumId
              standardVote
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
            }
          }
        }
    """.trimIndent()
}
