package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.request

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber

class DelegateStatsByAddressesRequest(recentVotesBlockThreshold: BlockNumber, val addresses: List<String>)  {
    val query = """
        query {
           delegates(filter:{accountId:{in:[${getAddresses()}]}}) {
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

    private fun getAddresses(): String {
        return addresses.joinToString { "\"$it\"" }
    }
}
