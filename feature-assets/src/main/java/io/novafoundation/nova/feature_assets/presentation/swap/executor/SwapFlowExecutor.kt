package io.novafoundation.nova.feature_assets.presentation.swap.executor

import io.novafoundation.nova.feature_assets.presentation.swap.SwapFlowPayload
import io.novafoundation.nova.feature_assets.presentation.swap.executor.ReselectSwapFlowExecutor.SelectingDirection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class SwapFlowExecutorFactory(
    private val initialSwapFlowExecutor: InitialSwapFlowExecutor,
    private val reselectAssetSwapFlowExecutorFactory: ReselectSwapFlowExecutorFactory
) {
    fun create(payload: SwapFlowPayload): SwapFlowExecutor {
        return when (payload.flowType) {
            SwapFlowPayload.FlowType.INITIAL_SELECTING -> initialSwapFlowExecutor
            SwapFlowPayload.FlowType.RESELECT_ASSET_OUT -> reselectAssetSwapFlowExecutorFactory.create(SelectingDirection.OUT)
            SwapFlowPayload.FlowType.SELECT_ASSET_IN -> reselectAssetSwapFlowExecutorFactory.create(SelectingDirection.IN)
        }
    }
}

interface SwapFlowExecutor {
    suspend fun openNextScreen(coroutineScope: CoroutineScope, chainAsset: Chain.Asset)
}
