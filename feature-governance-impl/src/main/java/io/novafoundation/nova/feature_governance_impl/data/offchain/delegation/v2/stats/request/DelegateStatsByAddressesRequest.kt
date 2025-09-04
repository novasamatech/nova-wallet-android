package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

import io.novafoundation.nova.feature_governance_api.data.repository.common.TimePoint
import io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.common.createSubqueryFilter

class DelegateStatsByAddressesRequest(timePointThreshold: TimePoint, val addresses: List<String>) {
    val query = """
        query {
           delegates(filter:{accountId:{in:[${getAddresses()}]}}) {
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

    private fun getAddresses(): String {
        return addresses.joinToString { "\"$it\"" }
    }
}
