package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

class StakingPeriodRewardsRequest(accountAddress: String, val startTimestamp: Long?, val endTimestamp: Long?) {
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

    private fun getTimestampFilter(): String {
        val start = startTimestamp?.let { "timestamp: { greaterThanOrEqualTo: \"$it\" }" }
        val end = endTimestamp?.let { "timestamp: { lessThanOrEqualTo: \"$it\" }" }
        return if (startTimestamp != null && endTimestamp != null) {
            "$start and: { $end }"
        } else {
            start ?: end ?: ""
        }
    }
}
