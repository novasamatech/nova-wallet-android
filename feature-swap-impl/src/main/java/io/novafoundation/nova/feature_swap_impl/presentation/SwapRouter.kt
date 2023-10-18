package io.novafoundation.nova.feature_swap_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface SwapRouter : ReturnableRouter {

    fun openSwapConfirmation()

    fun selectAssetIn(selectedAsset: FullChainAssetId?)

    fun selectAssetOut(selectedAsset: FullChainAssetId?)
}
