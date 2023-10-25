package io.novafoundation.nova.feature_swap_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface SwapRouter : ReturnableRouter {

    fun openSwapConfirmation()

    fun selectAssetIn(selectedAsset: AssetPayload?)

    fun selectAssetOut(selectedAsset: AssetPayload?)

    fun openSwapOptions()
}
