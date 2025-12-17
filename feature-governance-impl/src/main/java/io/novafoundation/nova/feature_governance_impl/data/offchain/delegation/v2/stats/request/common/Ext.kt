package io.novafoundation.nova.feature_governance_impl.data.offchain.delegation.v2.stats.request.common

import io.novafoundation.nova.feature_governance_api.data.repository.common.RecentVotesDateThreshold
import kotlin.time.Duration.Companion.milliseconds

fun RecentVotesDateThreshold.createSubqueryFilter(): String {
    return when (this) {
        is RecentVotesDateThreshold.BlockNumber -> "at: {greaterThanOrEqualTo: ${number.toLong()}}"
        is RecentVotesDateThreshold.Timestamp -> "timestamp: {greaterThanOrEqualTo: ${timestampMs.milliseconds.inWholeSeconds}}"
    }
}
