package io.novafoundation.nova.feature_swap_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface SwapRouter : ReturnableRouter {

    fun openSwapConfirmation()

    fun openSwapRoute()

    fun openSwapFee()

    fun openSwapExecution()

    fun selectAssetIn(selectedAsset: AssetPayload?)

    fun selectAssetOut(selectedAsset: AssetPayload?)

    fun openSwapOptions()

    fun openRetrySwap(payload: SwapSettingsPayload)

    fun openSendCrossChain(destination: AssetPayload, recipientAddress: String?)

    fun openReceive(assetPayload: AssetPayload)

    fun openBalanceDetails(assetPayload: AssetPayload)

    fun openBuyToken(chainId: String, assetId: Int)
}
