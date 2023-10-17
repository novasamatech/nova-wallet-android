package io.novafoundation.nova.feature_assets.presentation.swap

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapFlowExecutorFactory(
    private val initialSwapFlowExecutor: InitialSwapFlowExecutor,
    private val reselectAssetSwapFlowExecutor: InitialSwapFlowExecutor
) {
    fun create(payload: SwapFlowPayload): SwapFlowExecutor {
        return when (payload.flowType) {
            SwapFlowPayload.FlowType.INITIAL_SELECTING -> initialSwapFlowExecutor
            SwapFlowPayload.FlowType.RESELECT_ASSET -> reselectAssetSwapFlowExecutor
        }
    }
}

interface SwapFlowExecutor {
    fun openNextScreen(chainAsset: Chain.Asset)
}
