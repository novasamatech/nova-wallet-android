package io.novafoundation.nova.feature_assets.presentation.swap

import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class InitialSwapFlowExecutor(private val assetsRouter: AssetsRouter) : SwapFlowExecutor {

    override fun openNextScreen(chainAsset: Chain.Asset) {
        assetsRouter.openSwapSettings(chainAsset.chainId, chainAsset.id)
    }

}
