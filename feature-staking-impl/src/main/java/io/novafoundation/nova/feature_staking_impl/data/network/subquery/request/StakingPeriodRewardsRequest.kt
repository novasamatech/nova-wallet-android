package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

class StakingPeriodRewardsRequest(accountAddress: String, val startTimestamp: Long?, val endTimestamp: Long?) {
    val query = """
        query {
            start: accountRewards(
                filter: {
                    address: { equalTo: "$accountAddress"} 
                    ${startTimestampFilter()}
                }
                orderBy: BLOCK_NUMBER_ASC
                first: 1
            ) {
                nodes {
                    accumulatedAmount
                    amount
                }
            }
            
            end: accountRewards(
                filter: {
                    address: { equalTo : "$accountAddress" }
                    ${endTimestampFilter()}
                }
                orderBy: BLOCK_NUMBER_DESC
                first: 1
            ) {
                nodes {
                    accumulatedAmount
                    amount
                }
            }
        }
    """.trimIndent()

    private fun startTimestampFilter(): String {
        if (startTimestamp != null && endTimestamp != null) {
            return getTimestampRangeFilter()
        } else if (startTimestamp != null) {
            return "timestamp: { greaterThanOrEqualTo: \"$startTimestamp\" }"
        }

        return ""
    }

    private fun endTimestampFilter(): String {
        if (startTimestamp != null && endTimestamp != null) {
            return getTimestampRangeFilter()
        } else if (endTimestamp != null) {
            return "timestamp: { lessThanOrEqualTo: \"$endTimestamp\" }"
        }

        return ""
    }

    private fun getTimestampRangeFilter(): String {
        return "timestamp: { greaterThanOrEqualTo: \"$startTimestamp\" } and: { timestamp: { lessThanOrEqualTo: \"$endTimestamp\" } }"
    }
}
