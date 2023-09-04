package io.novafoundation.nova.feature_staking_impl.data.network.subquery.request

abstract class BaseStakingPeriodRewardsRequest(@Transient private val startTimestamp: Long?, @Transient private val endTimestamp: Long?) {

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
