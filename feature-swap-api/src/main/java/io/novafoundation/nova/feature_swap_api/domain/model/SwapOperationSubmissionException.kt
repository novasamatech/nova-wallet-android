package io.novafoundation.nova.feature_swap_api.domain.model

sealed class SwapOperationSubmissionException : Throwable() {

    class SimulationFailed : SwapOperationSubmissionException()
}
