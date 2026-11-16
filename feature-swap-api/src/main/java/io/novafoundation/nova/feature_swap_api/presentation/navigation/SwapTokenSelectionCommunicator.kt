package io.novafoundation.nova.feature_swap_api.presentation.navigation

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.parcelize.Parcelize

interface SwapTokenSelectionCommunicator : InterScreenCommunicator<SwapTokenSelectionCommunicator.Request, SwapTokenSelectionCommunicator.Response> {

    @Parcelize
    data class Request(val requestId: String, val selectingPay: Boolean, val oppositeAsset: AssetPayload?) : Parcelable

    @Parcelize
    data class Response(val requestId: String, val selectingPay: Boolean, val asset: AssetPayload) : Parcelable
}
