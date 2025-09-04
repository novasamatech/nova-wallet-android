package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.feature_governance_api.data.repository.common.TimePoint
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.common.createSubqueryFilter

class DelegateStatsRequest(timePointThreshold: TimePoint) {
    val query = """
        query {
           delegates {
              totalCount
              nodes {
                accountId
                delegators
                delegatorVotes
                delegateVotes(filter: {${timePointThreshold.createSubqueryFilter()}}) {
                  totalCount
                }
              }
           }
        }
    """.trimIndent()
}
