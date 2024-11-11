package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.sum
import kotlin.time.Duration

@JvmInline
value class SwapExecutionEstimate(val atomicOperationsEstimates: List<Duration>)

fun SwapExecutionEstimate.totalTime(): Duration {
    return atomicOperationsEstimates.sum()
}

fun SwapExecutionEstimate.remainingTimeWhenExecuting(stepIndex: Int): Duration {
    return atomicOperationsEstimates.drop(stepIndex).sum()
}
