package io.novafoundation.nova.feature_assets.presentation.swap.executor

import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class InitialSwapFlowExecutor(private val assetsRouter: AssetsRouter) : SwapFlowExecutor {

    override suspend fun openNextScreen(coroutineScope: CoroutineScope, chainAsset: Chain.Asset) {
        assetsRouter.openSwapSettings(AssetPayload(chainAsset.chainId, chainAsset.id))
    }
}
