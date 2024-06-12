package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AtomicSwapOperation {

    suspend fun estimateFee(): AtomicSwapOperationFee

    suspend fun submit(previousStepCorrection: SwapExecutionCorrection?): Result<SwapExecutionCorrection>
}

class AtomicSwapOperationArgs(
    val swapLimit: SwapLimit,
    val customFeeAsset: Chain.Asset?,
    val nativeAsset: Asset,
)

class AtomicSwapOperationFee(
    networkFee: Fee,
    val minimumBalanceBuyIn: MinimumBalanceBuyIn
) : Fee by networkFee

class SwapExecutionCorrection(
    val actualFee: Balance,
    val actualReceivedAmount: Balance,
    val submission: ExtrinsicSubmission,
    // TODO we may potentially adjust slippage as well...
)
