package io.novafoundation.nova.feature_swap_api.domain.model

import kotlin.time.Duration

@JvmInline
value class SwapExecutionEstimate(val atomicOperationsEstimates: List<Duration>)

fun SwapExecutionEstimate.totalTime(): Duration {
    return atomicOperationsEstimates.reduce { acc, next -> acc + next }
}
