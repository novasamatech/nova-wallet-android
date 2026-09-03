package io.novafoundation.nova.feature_staking_impl.presentation.common.analytics

/**
 * Short, low-cardinality failure code suitable for analytics.
 * Exception messages are intentionally not used since they may contain addresses, amounts and other unbounded data.
 */
fun Throwable.toAnalyticsFailureReason(): String = javaClass.simpleName
