package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber

class DelegateStatsRequest(recentVotesBlockThreshold: BlockNumber) {
    val query = """
        query {
           delegates {
              totalCount
              nodes {
                accountId
                delegators
                delegatorVotes
                delegateVotes(filter: {at: {greaterThanOrEqualTo: ${recentVotesBlockThreshold.toInt()}}}) {
                  totalCount
                }
              }
           }
        }
    """.trimIndent()
}
