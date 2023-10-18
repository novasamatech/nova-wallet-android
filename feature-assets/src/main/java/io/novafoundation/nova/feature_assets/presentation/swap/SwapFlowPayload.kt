package io.novafoundation.nova.feature_assets.presentation.swap

import android.os.Parcelable
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import kotlinx.android.parcel.Parcelize

@Parcelize
class SwapFlowPayload(val flowType: FlowType, val selectedAsset: AssetPayload? = null) : Parcelable {

    enum class FlowType {
        INITIAL_SELECTING,
        RESELECT_ASSET_OUT,
        SELECT_ASSET_IN,
    }
}
