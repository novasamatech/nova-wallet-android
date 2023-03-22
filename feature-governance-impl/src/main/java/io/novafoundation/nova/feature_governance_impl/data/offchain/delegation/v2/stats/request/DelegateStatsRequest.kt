package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

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
