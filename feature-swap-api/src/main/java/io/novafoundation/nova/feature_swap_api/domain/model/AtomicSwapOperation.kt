package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AtomicSwapOperation {

    suspend fun estimateFee(): AtomicSwapOperationFee

    suspend fun submit(previousStepCorrection: SwapExecutionCorrection?): Result<SwapExecutionCorrection>
}

class AtomicSwapOperationArgs(
    val swapLimit: SwapLimit,
    val customFeeAsset: Chain.Asset?,
)

typealias AtomicSwapOperationFee = Fee

// TODO this will later be used to perform more accurate non-atomic swaps
// So next segments can correct tx args based on outcome of previous segments
//class SwapExecutionCorrection(
//    val actualFee: Balance,
//    val actualReceivedAmount: Balance,
//    val submission: ExtrinsicSubmission,
//)

class SwapExecutionCorrection()
