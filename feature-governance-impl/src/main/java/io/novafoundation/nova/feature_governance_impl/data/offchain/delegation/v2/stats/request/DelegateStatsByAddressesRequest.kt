package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.feature_governance_api.data.repository.common.RecentVotesDateThreshold
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.common.createSubqueryFilter

class DelegateStatsByAddressesRequest(recentVotesDateThreshold: RecentVotesDateThreshold, val addresses: List<String>) {
    val query = """
        query {
           delegates(filter:{accountId:{in:[${getAddresses()}]}}) {
              totalCount
              nodes {
                accountId
                delegators
                delegatorVotes
                delegateVotes(filter: {${recentVotesDateThreshold.createSubqueryFilter()}}) {
                  totalCount
                }
              }
           }
        }
    """.trimIndent()

    private fun getAddresses(): String {
        return addresses.joinToString { "\"$it\"" }
    }
}
