package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request

class DelegateDelegatorsRequest(delegateAddress: String) {
    val query = """
        query {
          delegations(filter: {delegateId: {equalTo: "$delegateAddress" }}) {
            nodes {
              delegator
              delegation
            }
          }
        }
    """.trimIndent()
}
