package io.novafoundation.nova.feature_assets.presentation.swap.asset

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.parcelize.Parcelize

sealed class SwapFlowPayload : Parcelable {

    @Parcelize
    object InitialSelecting : SwapFlowPayload()

    @Parcelize
    class ReselectAssetOut(val selectedAssetIn: AssetPayload?) : SwapFlowPayload()

    @Parcelize
    class ReselectAssetIn(val selectedAssetOut: AssetPayload?) : SwapFlowPayload()
}

val SwapFlowPayload.constraintDirectionsAsset: AssetPayload?
    get() = when (this) {
        SwapFlowPayload.InitialSelecting -> null
        is SwapFlowPayload.ReselectAssetIn -> selectedAssetOut
        is SwapFlowPayload.ReselectAssetOut -> selectedAssetIn
    }
