package io.novafoundation.nova.feature_assets.presentation.swap

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class SwapFlowPayload(val flowType: FlowType) : Parcelable {

    enum class FlowType {
        INITIAL_SELECTING,
        RESELECT_ASSET,
    }
}
