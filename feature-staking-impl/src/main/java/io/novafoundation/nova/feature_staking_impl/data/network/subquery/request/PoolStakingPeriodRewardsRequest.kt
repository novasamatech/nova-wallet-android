package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

class PoolStakingPeriodRewardsRequest(accountAddress: String, startTimestamp: Long?, endTimestamp: Long?) :
    BaseStakingPeriodRewardsRequest(startTimestamp, endTimestamp) {
    val query = """
        query {
            rewards: accountPoolRewards(
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
            
            slashes: accountPoolRewards(
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
