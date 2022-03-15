package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

class StakingSumRewardRequest(accountAddress: String) {
    val query = """
        query {
            accumulatedRewards (
               filter: {
                    id: { equalTo: "$accountAddress"}  
               }  
            ) {
                nodes {
                    amount
                }
            }
        }
    """.trimIndent()
}
