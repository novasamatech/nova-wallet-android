package io.novafoundation.nova.feature_swap_impl.presentation.execution.model

import kotlin.time.Duration

sealed class SwapProgressModel {

    class InProgress(
        val stepDescription: String,
        val remainingTime: Duration,
        val operationsLabel: String
    ) : SwapProgressModel()

    class Completed(val at: String, val operationsLabel: String) : SwapProgressModel()

    class Failed(val reason: String, val at: String) : SwapProgressModel()
}
