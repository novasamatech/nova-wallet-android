package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

abstract class BaseStakingPeriodRewardsRequest(private val startTimestamp: Long?, private val endTimestamp: Long?) {

    protected fun getTimestampFilter(): String {
        val start = startTimestamp?.let { "timestamp: { greaterThanOrEqualTo: \"$it\" }" }
        val end = endTimestamp?.let { "timestamp: { lessThanOrEqualTo: \"$it\" }" }
        return if (startTimestamp != null && endTimestamp != null) {
            "$start and: { $end }"
        } else {
            start ?: end ?: ""
        }
    }
}
