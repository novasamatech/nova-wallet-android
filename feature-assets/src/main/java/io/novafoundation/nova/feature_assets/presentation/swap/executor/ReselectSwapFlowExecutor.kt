package io.novafoundation.nova.feature_assets.presentation.swap.executor

import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class ReselectSwapFlowExecutor(
    private val request: SwapTokenSelectionCommunicator.Request,
    private val communicator: SwapTokenSelectionCommunicator,
) : SwapFlowExecutor {

    override suspend fun openNextScreen(coroutineScope: CoroutineScope, chainAsset: Chain.Asset) {
        communicator.respond(SwapTokenSelectionCommunicator.Response(request.requestId, request.selectingPay, chainAsset.toAssetPayload()))
    }
}
