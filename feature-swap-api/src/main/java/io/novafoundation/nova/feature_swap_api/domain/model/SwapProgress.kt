package io.novafoundation.nova.feature_swap_api.domain.model

sealed class SwapProgress {

    class StepStarted(val step: String): SwapProgress()

    class Failure(val error: Throwable): SwapProgress()

    object Done: SwapProgress()
}
