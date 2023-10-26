package io.novafoundation.nova.feature_assets.presentation.swap.executor

import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.swap.SwapFlowPayload
import io.novafoundation.nova.feature_assets.presentation.swap.executor.ReselectSwapFlowExecutor.SelectingDirection
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class SwapFlowExecutorFactory(
    private val initialSwapFlowExecutor: InitialSwapFlowExecutor,
    private val assetsRouter: AssetsRouter,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val chainRegistry: ChainRegistry
) {
    fun create(payload: SwapFlowPayload): SwapFlowExecutor {
        return when (payload.flowType) {
            SwapFlowPayload.FlowType.INITIAL_SELECTING -> initialSwapFlowExecutor
            SwapFlowPayload.FlowType.RESELECT_ASSET_OUT -> createReselectFlowExecutor(SelectingDirection.OUT)
            SwapFlowPayload.FlowType.SELECT_ASSET_IN -> createReselectFlowExecutor(SelectingDirection.IN)
        }
    }

    private fun createReselectFlowExecutor(selectingDirection: SelectingDirection): ReselectSwapFlowExecutor {
        return ReselectSwapFlowExecutor(
            assetsRouter,
            swapSettingsStateProvider,
            selectingDirection,
            chainRegistry
        )
    }
}

interface SwapFlowExecutor {
    suspend fun openNextScreen(coroutineScope: CoroutineScope, chainAsset: Chain.Asset)
}
