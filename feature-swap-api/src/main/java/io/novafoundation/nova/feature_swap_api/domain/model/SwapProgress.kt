package io.novafoundation.nova.feature_swap_api.domain.model

sealed class SwapProgress {

    class StepStarted(val step: SwapProgressStep) : SwapProgress()

    class TransactionTracked(val step: SwapProgressStep, val transaction: SwapSubmittedTransaction) : SwapProgress()

    class Failure(
        val error: Throwable,
        val attemptedStep: SwapProgressStep,
        val transaction: SwapSubmittedTransaction? = null,
    ) : SwapProgress()

    data object Done : SwapProgress()
}

class SwapProgressStep(
    val index: Int,
    val displayData: AtomicOperationDisplayData,
    val operation: AtomicSwapOperation
)

/** A receipt observed on the sending chain; successful XCM submission alone does not prove delivery. */
data class SwapSubmittedTransaction(val chainId: String, val hash: String, val failedOnChain: Boolean)
