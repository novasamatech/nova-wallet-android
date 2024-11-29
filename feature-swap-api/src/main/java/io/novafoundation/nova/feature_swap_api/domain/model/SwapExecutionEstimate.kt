package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.sum
import kotlin.time.Duration

class SwapExecutionEstimate(
    val atomicOperationsEstimates: List<Duration>,
    val additionalBuffer: Duration
)

fun SwapExecutionEstimate.totalTime(): Duration {
    return remainingTimeWhenExecuting(stepIndex = 0)
}

fun SwapExecutionEstimate.remainingTimeWhenExecuting(stepIndex: Int): Duration {
    return atomicOperationsEstimates.drop(stepIndex).sum() + additionalBuffer
}
