package io.novafoundation.nova.feature_assets.presentation.swap.executor

import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsStateProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class ReselectSwapFlowExecutor(
    private val assetsRouter: AssetsRouter,
    private val swapSettingsStateProvider: SwapSettingsStateProvider,
    private val selectingDirection: SelectingDirection,
    private val chainRegistry: ChainRegistry
) : SwapFlowExecutor {

    enum class SelectingDirection {
        IN, OUT
    }

    override suspend fun openNextScreen(coroutineScope: CoroutineScope, chainAsset: Chain.Asset) {
        val state = swapSettingsStateProvider.getSwapSettingsState(coroutineScope)
        when (selectingDirection) {
            SelectingDirection.IN -> state.setAssetInUpdatingFee(chainAsset)
            SelectingDirection.OUT -> state.setAssetOut(chainAsset)
        }
        assetsRouter.back()
    }
}
