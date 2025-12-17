package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.feature_governance_api.data.repository.common.RecentVotesDateThreshold
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.common.createSubqueryFilter

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

class DirectHistoricalVotesRequest(address: String, recentVotesDateThreshold: RecentVotesDateThreshold) {

    val query = """
        query {
          castingVotings(filter: { and: { voter: {equalTo: "$address"}, ${recentVotesDateThreshold.createSubqueryFilter()}}}) {
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
