package io.novafoundation.nova.feature_assets.presentation.swap.network

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapFlowPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class NetworkSwapFlowPayload(
    val networkFlowPayload: NetworkFlowPayload,
    val swapFlowPayload: SwapFlowPayload
) : Parcelable
