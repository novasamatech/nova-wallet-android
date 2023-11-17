package io.novafoundation.nova.feature_swap_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.payload.SwapConfirmationPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface SwapRouter : ReturnableRouter {

    fun openSwapConfirmation(payload: SwapConfirmationPayload)

    fun selectAssetIn(selectedAsset: AssetPayload?)

    fun selectAssetOut(selectedAsset: AssetPayload?)

    fun openSwapOptions()

    fun openSendCrossChain(destination: AssetPayload, recipientAddress: String?)

    fun openReceive(assetPayload: AssetPayload)

    fun openBalanceDetails(assetPayload: AssetPayload)
}
