package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber

class DelegateDetailedStatsRequest(delegateAddress: String, recentVotesBlockThreshold: BlockNumber) {
    val query = """
        query {
           delegates(filter: {accountId: {equalTo: "$delegateAddress"}}) {
              nodes {
                accountId
                delegators
                delegatorVotes
                allVotes: delegateVotes {
                  totalCount
                }
                recentVotes: delegateVotes(filter: {at: {greaterThanOrEqualTo: $recentVotesBlockThreshold}}) {
                  totalCount
                }
              }
           }
        }
    """.trimIndent()
}
