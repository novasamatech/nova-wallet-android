package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.common

import io.novafoundation.nova.feature_governance_api.data.repository.common.TimePoint
import kotlin.time.Duration.Companion.milliseconds

fun TimePoint.createSubqueryFilter(): String {
    return when (this) {
        is TimePoint.BlockNumber -> "at: {greaterThanOrEqualTo: ${number.toLong()}}"
        is TimePoint.Timestamp -> "timestamp: {greaterThanOrEqualTo: ${timestampMs.milliseconds.inWholeSeconds}}"
    }
}
