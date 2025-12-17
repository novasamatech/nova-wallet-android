package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.feature_governance_api.data.repository.common.RecentVotesDateThreshold
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.common.createSubqueryFilter

class DelegateDetailedStatsRequest(delegateAddress: String, recentVotesDateThreshold: RecentVotesDateThreshold) {
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
                recentVotes: delegateVotes(filter: {${recentVotesDateThreshold.createSubqueryFilter()}}) {
                  totalCount
                }
              }
           }
        }
    """.trimIndent()
}
