package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

class DirectStakingPeriodRewardsRequest(accountAddress: String, val startTimestamp: Long?, val endTimestamp: Long?) :
    BaseStakingPeriodRewardsRequest(startTimestamp, endTimestamp) {
    val query = """
        query {
            rewards: accountRewards(
                filter: {
                    address: { equalTo : "$accountAddress" }
                    type: { equalTo: reward }
                    ${getTimestampFilter()}
                }
            ) {
                groupedAggregates(groupBy: ADDRESS) {
                    sum {
                        amount
                    }
                }
            }
            
            slashes: accountRewards(
                filter: {
                    address: { equalTo : "$accountAddress" }
                    type: { equalTo: slash }
                    ${getTimestampFilter()}
                }
            ) {
                groupedAggregates(groupBy: ADDRESS) {
                    sum {
                        amount
                    }
                }
            }
        }
    """.trimIndent()
}
