package io.novafoundation.nova.feature_assets.presentation.swap.asset

import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapTokenSelectionCommunicator
import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.parcelize.Parcelize

sealed class SwapFlowPayload : Parcelable {

    abstract val request: SwapTokenSelectionCommunicator.Request

    @Parcelize
    class ReselectAssetOut(val selectedAssetIn: AssetPayload?, override val request: SwapTokenSelectionCommunicator.Request) : SwapFlowPayload()

    @Parcelize
    class ReselectAssetIn(val selectedAssetOut: AssetPayload?, override val request: SwapTokenSelectionCommunicator.Request) : SwapFlowPayload()
}

val SwapFlowPayload.constraintDirectionsAsset: AssetPayload?
    get() = when (this) {
        is SwapFlowPayload.ReselectAssetIn -> selectedAssetOut
        is SwapFlowPayload.ReselectAssetOut -> selectedAssetIn
    }
