package io.novafoundation.nova.feature_assets.presentation.swap.executor

import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class InitialSwapFlowExecutor(private val assetsRouter: AssetsRouter) : SwapFlowExecutor {

    override suspend fun openNextScreen(coroutineScope: CoroutineScope, chainAsset: Chain.Asset) {
        val payload = SwapSettingsPayload.DefaultFlow(chainAsset.fullId.toAssetPayload())
        assetsRouter.openSwapSetupAmount(payload)
    }
}
