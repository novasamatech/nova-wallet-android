package io.novafoundation.nova.feature_swap_api.domain.model

sealed class SwapProgress {

    class StepStarted(val step: SwapProgressStep) : SwapProgress()

    class Failure(val error: Throwable, val attemptedStep: SwapProgressStep) : SwapProgress()

    object Done : SwapProgress()
}

class SwapProgressStep(
    val index: Int,
    val displayData: AtomicOperationDisplayData,
    val operation: AtomicSwapOperation
)
