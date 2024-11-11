package io.novafoundation.nova.feature_swap_impl.presentation.execution.model

import io.novafoundation.nova.common.utils.formatting.TimerValue

sealed class SwapProgressModel {

    class InProgress(
        val stepDescription: String,
        val remainingTime: TimerValue,
        val operationsLabel: String
    ): SwapProgressModel()

    class Completed(val at: String, val operationsLabel: String) : SwapProgressModel()

    class Failed(val reason: String, val at: String): SwapProgressModel()
}
