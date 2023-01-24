package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request

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

class DirectHistoricalVotesRequest(address: String) {

    val query = """
        query {
           castingVotings(filter: { voter: {equalTo: "$address"}}) {
            nodes {
              referendumId
              standardVote
            }
          }
        }
    """.trimIndent()
}
