package io.novafoundation.nova.feature_assets.presentation.swap.executor

import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapFlowPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class SwapFlowExecutorFactory(
    private val swapTokenSelectionCommunicator: SwapTokenSelectionCommunicator,
) {
    fun create(payload: SwapFlowPayload): SwapFlowExecutor {
        return ReselectSwapFlowExecutor(
            request = payload.request,
            communicator = swapTokenSelectionCommunicator,
        )
    }
}

interface SwapFlowExecutor {
    suspend fun openNextScreen(coroutineScope: CoroutineScope, chainAsset: Chain.Asset)
}
